### HTML SSE Demo

1. Build the project: `mvn clean install -pl osgi-bundles/bundles/logger`
2. Copy the JAR to `platform` plugin directory
3. Copy HTML content below to `<any-directory>`
4. Run this using `python3 -m http.server 3000 -d <any-directory>` or `npx serve -l 3000 <any-directory>`
5. Open `http://localhost:3000`, fill in the Base URL and your Kill Bill username/password, optionally the tenant API key/secret and the filters, then click **Connect**

### Filter combinations

Filters combine with AND. Empty fields are ignored. Entries lacking the filtered attribute are excluded.
`multitenant=false` servers skip the tenant checks (no `401`/`403`).

| API key + secret | accountId | userToken | Result |
|---|---|---|---|
| - | - | - | Every entry, all tenants |
| valid | - | - | Entries of that tenant |
| valid | valid UUID, known | - | Entries of that tenant and account |
| valid | valid UUID, unknown or other tenant's | - | Connected, heartbeats only |
| - | - | token | Entries with that token, all tenants |
| valid | - | token | Entries of that tenant with that token |
| valid | valid UUID, known | token | Entries of that tenant and account with that token |
| wrong, or only key or only secret | any | any | `401` |
| - | any `accountId` | any | `403` |
| valid | not a UUID | any | `400` |
| any | `accountId` or `userToken` given twice | any | `400` |

### HTML

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>SSE Logger Client</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: monospace; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
        h1 { font-size: 1.2em; margin-bottom: 16px; color: #569cd6; }
        .controls { display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 16px; }
        .field { display: flex; flex-direction: column; gap: 4px; }
        .field label { font-size: 0.8em; color: #9cdcfe; }
        .field input { background: #2d2d2d; border: 1px solid #3c3c3c; color: #d4d4d4; padding: 6px 10px; font-family: monospace; font-size: 0.9em; width: 260px; }
        .field input:focus { outline: none; border-color: #569cd6; }
        button { padding: 6px 16px; font-family: monospace; font-size: 0.9em; cursor: pointer; border: 1px solid #3c3c3c; }
        #connectBtn { background: #2ea043; color: #fff; border-color: #2ea043; }
        #connectBtn.connected { background: #d73a49; border-color: #d73a49; }
        #clearBtn { background: #2d2d2d; color: #d4d4d4; }
        .status { font-size: 0.8em; padding: 4px 8px; border-radius: 3px; align-self: center; }
        .status.open { color: #2ea043; }
        .status.closed { color: #d73a49; }
        #output { width: 100%; height: calc(100vh - 160px); background: #1a1a1a; border: 1px solid #3c3c3c; padding: 10px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; font-size: 0.85em; line-height: 1.5; }
        .msg-data { color: #ce9178; }
        .msg-heartbeat { color: #6a9955; }
        .msg-event { color: #569cd6; }
        .msg-id { color: #b5cea8; }
        .msg-system { color: #dcdcaa; font-style: italic; }
        .msg-error { color: #f48771; font-weight: bold; }
    </style>
</head>
<body>
    <h1>SSE Logger Client</h1>
    <div class="controls">
        <div class="field">
            <label>Base URL</label>
            <input id="baseUrl" value="http://localhost:8080/plugins/killbill-osgi-logger/" />
        </div>
        <div class="field">
            <label>Username</label>
            <input id="username" placeholder="admin" />
        </div>
        <div class="field">
            <label>Password</label>
            <input id="password" type="password" placeholder="password" />
        </div>
        <div class="field">
            <label>X-Killbill-ApiKey (tenant)</label>
            <input id="apiKey" placeholder="optional" />
        </div>
        <div class="field">
            <label>X-Killbill-ApiSecret (tenant)</label>
            <input id="apiSecret" type="password" placeholder="optional" />
        </div>
        <div class="field">
            <label>accountId (UUID, needs API key/secret)</label>
            <input id="accountId" placeholder="optional" />
        </div>
        <div class="field">
            <label>userToken</label>
            <input id="userToken" placeholder="optional" />
        </div>
        <button id="connectBtn">Connect</button>
        <button id="clearBtn">Clear</button>
        <span id="status" class="status closed">● Disconnected</span>
    </div>
    <div id="output"></div>

    <script type="text/javascript">
        // -- Vendored EventSource/eventsource v5.1.2 --
        // https://github.com/EventSource/eventsource
        // Bundled with esbuild (--bundle --format=iife --minify) so this file stays a single,
        // self-contained copy-paste artifact (no npm install / package.json needed). Unlike the
        // native browser EventSource, this implementation is fetch()-based and accepts a `fetch`
        // override, which is how we inject the Basic Auth header below.
        //
        // The MIT License: https://mit-license.org/ 
        // 
var KillbillEventSourcePolyfill=(()=>{var $=Object.defineProperty;var te=Object.getOwnPropertyDescriptor;var ne=Object.getOwnPropertyNames;var re=Object.prototype.hasOwnProperty;var ie=(r,t)=>{for(var i in t)$(r,i,{get:t[i],enumerable:!0})},se=(r,t,i,d)=>{if(t&&typeof t=="object"||typeof t=="function")for(let o of ne(t))!re.call(r,o)&&o!==i&&$(r,o,{get:()=>t[o],enumerable:!(d=te(t,o))||d.enumerable});return r};var oe=r=>se($({},"__esModule",{value:!0}),r);var ae={};ie(ae,{EventSource:()=>B});var O=class extends Event{code;message;constructor(t,i){super(t),this.code=i?.code??void 0,this.message=i?.message??void 0}[Symbol.for("nodejs.util.inspect.custom")](t,i,d){return d(M(this),i)}[Symbol.for("Deno.customInspect")](t,i){return t(M(this),i)}};function X(r){let t=globalThis.DOMException;return typeof t=="function"?new t(r,"SyntaxError"):new SyntaxError(r)}function N(r){return r instanceof Error?"errors"in r&&Array.isArray(r.errors)?r.errors.map(N).join(", "):"cause"in r&&r.cause instanceof Error?`${r}: ${N(r.cause)}`:r.message:`${r}`}function M(r){return{type:r.type,message:r.message,code:r.code,defaultPrevented:r.defaultPrevented,cancelable:r.cancelable,timeStamp:r.timeStamp}}var w=class extends Error{constructor(t,i){super(t),this.name="ParseError",this.type=i.type,this.field=i.field,this.value=i.value,this.line=i.line}};var P=10,F=13,y=32,H=6;function D(r){if(typeof r=="function")throw new TypeError("`config` must be an object, got a function instead. Did you mean `createParser({onEvent: fn})`?");let{maxBufferSize:t,onComment:i,onError:d,onEvent:o,onId:E,onRetry:I}=r,u=[],p=0,g="",f,l="",h=0,v,S=!1,C=!1,x=!1;function J(e){if(S)throw new Error("Cannot feed parser: it was terminated after exceeding the configured max buffer size. Call `reset()` to resume parsing.");if(g!==void 0){if(e=g+e,e===""||e==="\xEF"||e==="\xEF\xBB"){g=e;return}g=void 0,e=e.replace(/^(?:\uFEFF|\xEF\xBB\xBF)/,"")}if((C||x)&&(e=K(e),!e))return;if(!u.length){let s=z(e);s!==""&&j(s),b();return}if(e.indexOf(`
`)===-1&&e.indexOf("\r")===-1){if(p<H){let s=u.join("")+e.slice(0,H-p);if(!U(s)){u.length=0,p=0,C=!0;return}}u.push(e),p+=e.length,b();return}u.push(e);let n=u.join("");u.length=0,p=0,j(z(n)),b()}function K(e){if(e.length===0)return e;if(x)return x=!1,e.charCodeAt(0)===P?e.slice(1):e;let n=e.indexOf("\r"),s=e.indexOf(`
`),a=n===-1?s:s===-1||n<s?n:s;return a===-1?"":a===e.length-1&&e.charCodeAt(a)===F?(C=!1,x=!0,""):(C=!1,e.slice(a+(e.charCodeAt(a)===F&&e.charCodeAt(a+1)===P?2:1)))}function j(e){if(e){if(e.charCodeAt(e.length-1)===F){if(T(e,0,e.length-1),b())return;x=!0;return}if(U(e)){u.push(e),p=e.length;return}C=!0}}function U(e){let n=e.charCodeAt(0);return n===58&&!!i||n===100&&R(e,"data")||n===101&&R(e,"event")||n===105&&R(e,"id")||n===114&&R(e,"retry")}function b(){return S?!0:t===void 0||p+l.length<=t?!1:(S=!0,u.length=0,p=0,f=void 0,l="",h=0,v=void 0,C=!1,x=!1,d?.(new w(`Buffered data exceeded max buffer size of ${t} characters`,{type:"max-buffer-size-exceeded"})),!0)}function z(e){let n=0;if(e.indexOf("\r")===-1){let s=e.indexOf(`
`,n);for(;s!==-1;){if(n===s){f!==void 0&&E?.(f),h>0&&o?.({id:f,event:v,data:l}),f=void 0,l="",h=0,v=void 0,n=s+1,s=e.indexOf(`
`,n);continue}let a=e.charCodeAt(n);if(q(e,n,a)){let c=e.charCodeAt(n+5)===y?n+6:n+5,m=e.slice(c,s);if(h===0&&e.charCodeAt(s+1)===P){f!==void 0&&E?.(f),o?.({id:f,event:v,data:m}),f=void 0,l="",v=void 0,n=s+2,s=e.indexOf(`
`,n);continue}l=h===0?m:`${l}
${m}`,h++}else W(e,n,a)?v=e.slice(e.charCodeAt(n+6)===y?n+7:n+6,s)||void 0:T(e,n,s);if(b())return"";n=s+1,s=e.indexOf(`
`,n)}return e.slice(n)}for(;n<e.length;){let s=e.indexOf("\r",n),a=e.indexOf(`
`,n),c=-1;if(s!==-1&&a!==-1?c=s<a?s:a:s!==-1?s===e.length-1?c=-1:c=s:a!==-1&&(c=a),c===-1)break;if(T(e,n,c),b())return"";n=c+1,e.charCodeAt(n-1)===F&&e.charCodeAt(n)===P&&n++}return e.slice(n)}function T(e,n,s){if(n===s){Q();return}let a=e.charCodeAt(n);if(q(e,n,a)){let A=e.charCodeAt(n+5)===y?n+6:n+5,_=e.slice(A,s);l=h===0?_:`${l}
${_}`,h++;return}if(W(e,n,a)){v=e.slice(e.charCodeAt(n+6)===y?n+7:n+6,s)||void 0;return}if(a===105&&e.charCodeAt(n+1)===100&&e.charCodeAt(n+2)===58){let A=e.slice(e.charCodeAt(n+3)===y?n+4:n+3,s);A.includes("\0")||(f=A);return}if(a===58){if(i){let A=e.slice(n,s);i(A.slice(e.charCodeAt(n+1)===y?2:1))}return}let c=e.slice(n,s),m=c.indexOf(":");if(m===-1){G(c,"",c);return}let Y=c.slice(0,m),k=c.charCodeAt(m+1)===y?2:1,ee=c.slice(m+k);G(Y,ee,c)}function G(e,n,s){switch(e){case"event":v=n||void 0;break;case"data":l=h===0?n:`${l}
${n}`,h++;break;case"id":n.includes("\0")||(f=n);break;case"retry":/^\d+$/.test(n)?I?.(parseInt(n,10)):d?.(new w(`Invalid \`retry\` value: "${n}"`,{type:"invalid-retry",value:n,line:s}));break;default:d?.(new w(`Unknown field "${e.length>20?`${e.slice(0,20)}\u2026`:e}"`,{type:"unknown-field",field:e,value:n,line:s}));break}}function Q(){f!==void 0&&E?.(f),h>0&&o?.({id:f,event:v,data:l}),f=void 0,l="",h=0,v=void 0}function V(e={}){if(e.consume&&u.length>0){let n=u.join("");T(n,0,n.length)}g="",f=void 0,l="",h=0,v=void 0,u.length=0,p=0,S=!1,C=!1,x=!1}return{feed:J,reset:V}}function q(r,t,i){return i===100&&r.charCodeAt(t+1)===97&&r.charCodeAt(t+2)===116&&r.charCodeAt(t+3)===97&&r.charCodeAt(t+4)===58}function W(r,t,i){return i===101&&r.charCodeAt(t+1)===118&&r.charCodeAt(t+2)===101&&r.charCodeAt(t+3)===110&&r.charCodeAt(t+4)===116&&r.charCodeAt(t+5)===58}function R(r,t){let i=1;for(;i<r.length&&i<t.length;){if(r.charCodeAt(i)!==t.charCodeAt(i))return!1;i++}return r.length<=t.length||r.charCodeAt(t.length)===58}var de=100*1024*1024,L=class extends EventTarget{static CONNECTING=0;static OPEN=1;static CLOSED=2;CONNECTING=0;OPEN=1;CLOSED=2;get readyState(){return this.#e}get url(){return this.#n.href}get withCredentials(){return this.#h}get onerror(){return this.#f}set onerror(t){this.#f&&this.removeEventListener("error",this.#f),this.#f=t,t&&this.addEventListener("error",t)}get onmessage(){return this.#a}set onmessage(t){this.#a&&this.removeEventListener("message",this.#a),this.#a=t,t&&this.addEventListener("message",t)}get onopen(){return this.#l}set onopen(t){this.#l&&this.removeEventListener("open",this.#l),this.#l=t,t&&this.addEventListener("open",t)}addEventListener(t,i,d){let o=i;super.addEventListener(t,o,d)}removeEventListener(t,i,d){let o=i;super.removeEventListener(t,o,d)}constructor(t,i){super();try{if(t instanceof URL)this.#n=t;else if(typeof t=="string")this.#n=new URL(t,fe());else throw new Error("Invalid URL")}catch{throw X("An invalid or illegal string was specified")}this.#d=D({maxBufferSize:i?.maxBufferSize??de,onEvent:this.#x,onError:this.#b,onId:this.#C,onRetry:this.#y}),this.#e=this.CONNECTING,this.#c=3e3,this.#u=i?.fetch??globalThis.fetch,this.#h=i?.withCredentials??!1,this.#E()}close(){this.#s&&clearTimeout(this.#s),this.#e!==this.CLOSED&&(this.#t&&this.#t.abort(),this.#e=this.CLOSED,this.#t=void 0)}#e;#n;#i;#h;#u;#c;#s;#o=null;#t;#d;#f=null;#a=null;#l=null;#E(){this.#e=this.CONNECTING,this.#t=new AbortController;let t=this.#u;t(this.#n,this.#g()).then(this.#v).catch(this.#m)}#v=async t=>{this.#d.reset();let{body:i,redirected:d,status:o,headers:E}=t;if(o===204){this.#r("Server sent HTTP 204, not reconnecting",204),this.close();return}if(d?this.#i=new URL(t.url):this.#i=void 0,o!==200){this.#r(`Non-200 status code (${o})`,o);return}if(!(E.get("content-type")||"").startsWith("text/event-stream")){this.#r('Invalid content type, expected "text/event-stream"',o);return}if(this.#e===this.CLOSED)return;this.#e=this.OPEN;let u=new Event("open");if(this.dispatchEvent(u),typeof i!="object"||!i||!("getReader"in i)){this.#r("Invalid response body, expected a web ReadableStream",o),this.close();return}let p=new TextDecoder,g=i.getReader(),f=!0;do{let{done:l,value:h}=await g.read();if(this.#e===this.CLOSED){f=!1;break}h&&this.#d.feed(p.decode(h,{stream:!l})),l&&(f=!1,this.#d.reset(),this.#p())}while(f)};#m=t=>{this.#t=void 0,!(t.name==="AbortError"||t.type==="aborted")&&this.#p(N(t))};#g(){let i={mode:"cors",redirect:"follow",headers:{Accept:"text/event-stream",...this.#o?{"Last-Event-ID":this.#o}:void 0},cache:"no-store",signal:this.#t?.signal};return"window"in globalThis&&(i.credentials=this.withCredentials?"include":"same-origin"),i}#C=t=>{this.#o=t};#x=t=>{if(this.#e===this.CLOSED)return;let i=this.#i?this.#i.origin:this.#n.origin,d=this.#o??"",o=new MessageEvent(t.event||"message",{data:t.data,origin:i,lastEventId:d});o.origin!==i&&Z(o,"origin",i),o.lastEventId!==d&&Z(o,"lastEventId",d),this.dispatchEvent(o)};#y=t=>{this.#c=t};#b=t=>{t.type==="max-buffer-size-exceeded"&&(this.close(),this.#r(t.message))};#r(t,i){this.close();let d=new O("error",{code:i,message:t});this.dispatchEvent(d)}#p(t,i){if(this.#e===this.CLOSED)return;this.#e=this.CONNECTING;let d=new O("error",{code:i,message:t});this.dispatchEvent(d);let o=setTimeout(this.#A,this.#c);typeof o=="object"&&o!==null&&"unref"in o&&o.unref(),this.#s=o}#A=()=>{this.#s=void 0,this.#e===this.CONNECTING&&this.#E()}};Object.defineProperty(L,"name",{value:"EventSource"});Object.defineProperty(L,Symbol.for("eventsource.supports-fetch-override"),{value:!0,writable:!1,configurable:!1,enumerable:!1});var B=L;function fe(){let r="document"in globalThis?globalThis.document:void 0;return r&&typeof r=="object"&&"baseURI"in r&&typeof r.baseURI=="string"?r.baseURI:void 0}function Z(r,t,i){Object.defineProperty(r,t,{value:i,enumerable:!0,configurable:!0})}return oe(ae);})();
    </script>
    <script type="text/javascript">
        // -- SSE Client --
        let eventSource = null;

        function buildUrl() {
            const base = document.getElementById('baseUrl').value.replace(/\/$/, '');
            const accountId = document.getElementById('accountId').value.trim();
            const userToken = document.getElementById('userToken').value.trim();
            const params = [];
            if (accountId) params.push('accountId=' + encodeURIComponent(accountId));
            if (userToken) params.push('userToken=' + encodeURIComponent(userToken));
            return params.length > 0 ? base + '/?' + params.join('&') : base + '/';
        }

        function buildAuthHeader() {
            const username = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            if (!username && !password) return null;
            // btoa() only accepts Latin-1 (code points 0-255); the server decodes the credentials as
            // UTF-8, so non-Latin-1 characters (e.g. accented letters) would otherwise throw
            // InvalidCharacterError here. This UTF-8-safe encoding round-trips correctly either way.
            return 'Basic ' + utf8ToBase64(username + ':' + password);
        }

        // Tenant headers, only when both are filled in; a half-filled pair is sent as is so the
        // server's 401 for an incomplete pair can be observed.
        function buildTenantHeaders() {
            const apiKey = document.getElementById('apiKey').value.trim();
            const apiSecret = document.getElementById('apiSecret').value.trim();
            const headers = {};
            if (apiKey) headers['X-Killbill-ApiKey'] = apiKey;
            if (apiSecret) headers['X-Killbill-ApiSecret'] = apiSecret;
            return headers;
        }

        function utf8ToBase64(str) {
            const bytes = new TextEncoder().encode(str);
            let binary = '';
            bytes.forEach(function(b) { binary += String.fromCharCode(b); });
            return btoa(binary);
        }

        function appendMessage(text, className) {
            const output = document.getElementById('output');
            const line = document.createElement('div');
            line.className = className;
            line.textContent = '[' + new Date().toISOString().substring(11, 23) + '] ' + text;
            output.appendChild(line);
            output.scrollTop = output.scrollHeight;
        }

        function setStatus(connected) {
            const status = document.getElementById('status');
            const btn = document.getElementById('connectBtn');
            if (connected) {
                status.textContent = '● Connected';
                status.className = 'status open';
                btn.textContent = 'Disconnect';
                btn.classList.add('connected');
            } else {
                status.textContent = '● Disconnected';
                status.className = 'status closed';
                btn.textContent = 'Connect';
                btn.classList.remove('connected');
            }
        }

        function connect() {
            // Defensive: close any previous instance before starting a new one, in case it was
            // left dangling (see the onerror note below on why that could otherwise happen).
            if (eventSource) {
                eventSource.close();
                eventSource = null;
            }

            const url = buildUrl();
            const authHeader = buildAuthHeader();
            const tenantHeaders = buildTenantHeaders();
            appendMessage('Connecting to: ' + url, 'msg-system');

            // Custom fetch: injects the Basic Auth and tenant headers on every (re)connect attempt,
            // since the native EventSource API has no way to set request headers at all.
            const authFetch = function(input, init) {
                const headers = new Headers((init && init.headers) || {});
                if (authHeader) headers.set('Authorization', authHeader);
                Object.keys(tenantHeaders).forEach(function(name) { headers.set(name, tenantHeaders[name]); });
                return fetch(input, Object.assign({}, init, { headers: headers }));
            };

            eventSource = new KillbillEventSourcePolyfill.EventSource(url, { fetch: authFetch });

            eventSource.onopen = function() {
                setStatus(true);
                appendMessage('Connection opened', 'msg-system');
            };

            // Default message event (unnamed events / data-only)
            eventSource.onmessage = function(event) {
                let display = 'data: ' + event.data;
                if (event.lastEventId) display += '  [id: ' + event.lastEventId + ']';
                appendMessage(display, 'msg-data');
            };

            // Named "heartbeat" event
            eventSource.addEventListener('heartbeat', function(event) {
                let display = 'event: heartbeat';
                if (event.lastEventId) display += '  [id: ' + event.lastEventId + ']';
                appendMessage(display, 'msg-heartbeat');
            });

            // Note: no separate addEventListener('message', ...) here -- setting onmessage above
            // already registers a "message" listener internally (default/unnamed events AND
            // explicit event:message lines both dispatch as type "message"). Registering both would
            // double-fire and append every log entry twice.

            eventSource.onerror = function(event) {
                // Unlike native EventSource, this polyfill exposes the HTTP status code/message
                // (event.code / event.message). On an HTTP-level failure (e.g. 401 Unauthorized,
                // 429 Too Many Requests) it already closes itself before firing this event, per
                // spec, instead of looping forever like a buggy client would. On a transient/
                // network-level failure (no HTTP status -- e.g. the fetch() call itself rejected)
                // it does NOT close itself: it silently schedules its own internal reconnect retry.
                // We always explicitly close() here regardless of which case this is, so we never
                // leave a stale instance quietly reconnecting in the background after we've already
                // torn down the UI/state for it (which otherwise looks like "stuck/always connected"
                // and makes the Disconnect button appear to do nothing).
                const hints = {
                    400: 'accountId is not a UUID, or accountId/userToken is repeated',
                    401: 'bad Basic Auth credentials, or a wrong/incomplete API key + secret',
                    403: 'accountId requires the API key + secret',
                    429: 'too many failed attempts, retry later'
                };
                const hint = hints[event.code] ? ' - ' + hints[event.code] : '';
                const detail = event.code ? (' (HTTP ' + event.code + (event.message ? ': ' + event.message : '') + hint + ')') : '';
                appendMessage('Connection error' + detail, 'msg-error');
                if (eventSource) {
                    eventSource.close();
                }
                setStatus(false);
                eventSource = null;
            };
        }

        function disconnect() {
            if (eventSource) {
                eventSource.close();
                eventSource = null;
                setStatus(false);
                appendMessage('Disconnected by user', 'msg-system');
            }
        }

        // -- Event Handlers --
        document.getElementById('connectBtn').addEventListener('click', function() {
            if (eventSource) {
                disconnect();
            } else {
                connect();
            }
        });

        document.getElementById('clearBtn').addEventListener('click', function() {
            document.getElementById('output').innerHTML = '';
        });
    </script>
</body>
</html>
```
