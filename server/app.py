"""JL02 TV calendar pairing service. Deploy behind HTTPS; see README for production hardening."""
import os, sqlite3, secrets, hashlib, json, time, html, asyncio
from datetime import datetime, timedelta, timezone
from urllib.parse import urlencode
from zoneinfo import ZoneInfo
from contextlib import contextmanager
import httpx
from cryptography.fernet import Fernet
from fastapi import FastAPI, Header, HTTPException, Request
from fastapi.responses import HTMLResponse, RedirectResponse

BASE=os.environ.get('PUBLIC_BASE_URL','').rstrip('/')
DB=os.environ.get('SQLITE_PATH','jl02.sqlite3')
KEY=os.environ.get('FERNET_KEY','')
if not BASE.startswith('https://') or not KEY: raise RuntimeError('Configure HTTPS PUBLIC_BASE_URL and FERNET_KEY before starting')
crypt=Fernet(KEY.encode())
app=FastAPI(title='JL02 TV Calendar Service', docs_url=None, redoc_url=None)
PROVIDERS={
 'google':{'client':os.environ.get('GOOGLE_CLIENT_ID',''),'secret':os.environ.get('GOOGLE_CLIENT_SECRET',''), 'auth':'https://accounts.google.com/o/oauth2/v2/auth','token':'https://oauth2.googleapis.com/token','scope':'https://www.googleapis.com/auth/calendar.events.readonly'},
 'microsoft':{'client':os.environ.get('MS_CLIENT_ID',''),'secret':os.environ.get('MS_CLIENT_SECRET',''),'auth':'https://login.microsoftonline.com/common/oauth2/v2.0/authorize','token':'https://login.microsoftonline.com/common/oauth2/v2.0/token','scope':'offline_access Calendars.Read'},
}

def digest(v): return hashlib.sha256(v.encode()).hexdigest()
@contextmanager
def conn():
 c=sqlite3.connect(DB,timeout=20);c.row_factory=sqlite3.Row
 try: yield c;c.commit()
 finally:c.close()
def setup():
 with conn() as c:
  c.executescript('''CREATE TABLE IF NOT EXISTS devices(id TEXT PRIMARY KEY,token_hash TEXT NOT NULL,created INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS pairings(ticket_hash TEXT PRIMARY KEY,device_id TEXT NOT NULL,expires INTEGER NOT NULL,used INTEGER DEFAULT 0);
CREATE TABLE IF NOT EXISTS oauth_states(state_hash TEXT PRIMARY KEY,ticket_hash TEXT NOT NULL,provider TEXT NOT NULL,expires INTEGER NOT NULL);
CREATE TABLE IF NOT EXISTS accounts(id TEXT PRIMARY KEY,device_id TEXT NOT NULL,provider TEXT NOT NULL,tokens BLOB NOT NULL,created INTEGER NOT NULL);''')
setup()

def device(auth):
 if not auth or not auth.startswith('Bearer '): raise HTTPException(401,'Device authorization required')
 with conn() as c: row=c.execute('SELECT id FROM devices WHERE token_hash=?',(digest(auth[7:]),)).fetchone()
 if not row:raise HTTPException(401,'Unknown device')
 return row['id']

def page(title,body):
 return HTMLResponse('<!doctype html><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="referrer" content="no-referrer"><style>body{font:17px system-ui;background:#101927;color:#fff;max-width:480px;margin:55px auto;padding:20px}a,button{display:block;background:#126c62;color:#fff;text-decoration:none;padding:16px;border-radius:12px;margin:14px 0;border:0}small{color:#aab}</style><h2>'+html.escape(title)+'</h2>'+body)

@app.get('/health')
def health():
 # No secrets or sensitive account information are exposed.
 return {'service':'JL02 TV Calendar Service','status':'ok',
         'google_configured':bool(PROVIDERS['google']['client'] and PROVIDERS['google']['secret']),
         'microsoft_configured':bool(PROVIDERS['microsoft']['client'] and PROVIDERS['microsoft']['secret'])}

@app.post('/v1/device')
def register():
 did=secrets.token_urlsafe(24);token=secrets.token_urlsafe(48)
 with conn() as c:c.execute('INSERT INTO devices VALUES(?,?,?)',(did,digest(token),int(time.time())))
 return {'device_token':token}

@app.post('/v1/pair')
def pair(authorization:str=Header(default='')):
 did=device(authorization); ticket=secrets.token_urlsafe(32)
 with conn() as c:c.execute('INSERT INTO pairings(ticket_hash,device_id,expires) VALUES(?,?,?)',(digest(ticket),did,int(time.time())+600))
 return {'url':BASE+'/connect/'+ticket,'expires_in':600}

@app.get('/connect/{ticket}', response_class=HTMLResponse)
def connect(ticket:str):
 with conn() as c:r=c.execute('SELECT * FROM pairings WHERE ticket_hash=?',(digest(ticket),)).fetchone()
 if not r or r['expires']<time.time() or r['used']:return page('Link expired','<p>Return to JL02 TV for a new connection link.</p>')
 buttons=''.join('<a href="/auth/'+p+'/'+ticket+'">Connect '+('Google Calendar' if p=='google' else 'Microsoft Outlook')+'</a>' for p in PROVIDERS)
 return page('Connect to JL02 TV','<p>Only approve sign-in if you initiated this from your own television.</p>'+buttons+'<small>Read-only calendar access. You can disconnect the account on your TV.</small>')

@app.get('/auth/{provider}/{ticket}')
def auth(provider:str,ticket:str):
 cfg=PROVIDERS.get(provider)
 if not cfg or not cfg['client'] or not cfg['secret']:raise HTTPException(503,'Provider not configured')
 with conn() as c:r=c.execute('SELECT * FROM pairings WHERE ticket_hash=?',(digest(ticket),)).fetchone()
 if not r or r['expires']<time.time() or r['used']:raise HTTPException(410,'Pairing expired')
 state=secrets.token_urlsafe(32)
 with conn() as c:c.execute('INSERT INTO oauth_states VALUES(?,?,?,?)',(digest(state),digest(ticket),provider,int(time.time())+600))
 params={'client_id':cfg['client'],'response_type':'code','redirect_uri':BASE+'/callback/'+provider,'scope':cfg['scope'],'state':state}
 if provider=='google':params.update(access_type='offline',prompt='consent')
 return RedirectResponse(cfg['auth']+'?'+urlencode(params),status_code=302)

@app.get('/callback/{provider}')
async def callback(provider:str,code:str='',state:str='',error:str=''):
 if error:return page('Authorization cancelled','<p>'+html.escape(error)+'</p>')
 if provider not in PROVIDERS or not code or not state:raise HTTPException(400,'Missing authorization values')
 with conn() as c:
  r=c.execute('SELECT * FROM oauth_states WHERE state_hash=? AND provider=?',(digest(state),provider)).fetchone()
  if not r or r['expires']<time.time():raise HTTPException(400,'Authorization expired')
  pair=c.execute('SELECT * FROM pairings WHERE ticket_hash=?',(r['ticket_hash'],)).fetchone()
  if not pair or pair['expires']<time.time() or pair['used']:raise HTTPException(410,'Pairing expired')
  c.execute('DELETE FROM oauth_states WHERE state_hash=?',(digest(state),))
 cfg=PROVIDERS[provider]
 async with httpx.AsyncClient(timeout=20) as h:
  resp=await h.post(cfg['token'],data={'client_id':cfg['client'],'client_secret':cfg['secret'],'code':code,'grant_type':'authorization_code','redirect_uri':BASE+'/callback/'+provider})
  if resp.status_code!=200:raise HTTPException(502,'OAuth token exchange failed')
  token=resp.json()
 if not token.get('refresh_token'):raise HTTPException(400,'Refresh token unavailable; revoke prior consent and reconnect')
 with conn() as c:
  changed=c.execute('UPDATE pairings SET used=1 WHERE ticket_hash=? AND used=0',(r['ticket_hash'],)).rowcount
  if not changed:raise HTTPException(409,'Pairing already completed')
  c.execute('INSERT INTO accounts VALUES(?,?,?,?,?)',(secrets.token_urlsafe(18),pair['device_id'],provider,crypt.encrypt(json.dumps(token).encode()),int(time.time())))
 return page('Calendar connected','<p>Your '+html.escape(provider.title())+' calendar is now linked. Return to JL02 TV and select Refresh.</p><small>You can connect another account from the TV.</small>')

async def access_token(account):
 cfg=PROVIDERS[account['provider']]; token=json.loads(crypt.decrypt(account['tokens']).decode())
 if token.get('_expires_at',0)>time.time()+90:return token['access_token']
 # Older tokens may lack expires_at; refreshing is safe and server-side.
 async with httpx.AsyncClient(timeout=20) as h:
  r=await h.post(cfg['token'],data={'client_id':cfg['client'],'client_secret':cfg['secret'],'grant_type':'refresh_token','refresh_token':token['refresh_token'],'scope':cfg['scope']})
  r.raise_for_status();new=r.json()
 token.update(new)
 if 'refresh_token' not in new:token['refresh_token']=json.loads(crypt.decrypt(account['tokens']).decode())['refresh_token']
 token['_expires_at']=int(time.time())+int(new.get('expires_in',3600))
 with conn() as c:c.execute('UPDATE accounts SET tokens=? WHERE id=?',(crypt.encrypt(json.dumps(token).encode()),account['id']))
 return token['access_token']

async def get_events(account,start,end):
 access=await access_token(account); headers={'Authorization':'Bearer '+access};provider=account['provider']
 async with httpx.AsyncClient(timeout=20) as h:
  if provider=='google':
   resp=await h.get('https://www.googleapis.com/calendar/v3/calendars/primary/events',headers=headers,params={'timeMin':start.isoformat(),'timeMax':end.isoformat(),'singleEvents':'true','orderBy':'startTime','maxResults':100})
  else:
   resp=await h.get('https://graph.microsoft.com/v1.0/me/calendarView',headers={**headers,'Prefer':'outlook.timezone="Asia/Kuala_Lumpur"'},params={'startDateTime':start.isoformat(),'endDateTime':end.isoformat(),'$top':100,'$select':'subject,start,end,isAllDay,sensitivity,isCancelled'})
  resp.raise_for_status();data=resp.json()
 result=[]
 for e in data.get('items' if provider=='google' else 'value',[]):
  if e.get('status')=='cancelled' or e.get('isCancelled'):continue
  if provider=='google':
   when=e.get('start',{}).get('dateTime') or e.get('start',{}).get('date','');title=e.get('summary','Busy');private=e.get('visibility') in ('private','confidential');allday='date' in e.get('start',{})
  else:
   when=e.get('start',{}).get('dateTime','');title=e.get('subject','Busy');private=e.get('sensitivity') in ('private','confidential');allday=e.get('isAllDay',False)
  result.append({'time':('All day' if allday else when[11:16]),'title':('Private appointment' if private else title),'private':private,'provider':provider})
 return result

@app.get('/v1/events')
async def events(authorization:str=Header(default='')):
 did=device(authorization);tz=ZoneInfo('Asia/Kuala_Lumpur');now=datetime.now(tz);start=now.replace(hour=0,minute=0,second=0,microsecond=0);end=start+timedelta(days=1)
 with conn() as c:accounts=c.execute('SELECT * FROM accounts WHERE device_id=?',(did,)).fetchall()
 all_events=[];errors=[]
 for a in accounts:
  try:all_events+=await get_events(a,start,end)
  except Exception:errors.append(a['provider']+' sync failed')
 all_events.sort(key=lambda e:e['time'] if e['time']!='All day' else '00:00')
 return {'connected_accounts':len(accounts),'events':all_events[:80],'errors':errors,'updated_at':now.isoformat()}

@app.get('/v1/accounts')
def accounts(authorization:str=Header(default='')):
 did=device(authorization)
 with conn() as c:rows=c.execute('SELECT id,provider FROM accounts WHERE device_id=?',(did,)).fetchall()
 return {'accounts':[dict(r) for r in rows]}

@app.delete('/v1/accounts/{account_id}')
def disconnect(account_id:str,authorization:str=Header(default='')):
 did=device(authorization)
 with conn() as c:c.execute('DELETE FROM accounts WHERE id=? AND device_id=?',(account_id,did))
 return {'disconnected':True}

@app.delete('/v1/device')
def unlink_device(authorization:str=Header(default='')):
 did=device(authorization)
 with conn() as c:
  c.execute('DELETE FROM accounts WHERE device_id=?',(did,));c.execute('DELETE FROM pairings WHERE device_id=?',(did,));c.execute('DELETE FROM devices WHERE id=?',(did,))
 return {'unlinked':True}
