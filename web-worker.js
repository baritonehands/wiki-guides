importScripts("shared.js");
(function(){
'use strict';var X0=function(a){a=$APP.yE(a);return self.postMessage({type:"fetch",payload:W0.write(a)})};var W0=$APP.PA();var Y0=$APP.Nv(1E3);var Z0=$APP.hA();(function(){for(var a=0;;)if(12>a){var b=$APP.Nv(1);$APP.rv(function(c,d,e){return function(){var f=function(){return function(k,l){return function(){function n(v){for(;;){a:try{for(;;){var A=l(v);if(!$APP.Q(A,$APP.pB)){var D=A;break a}}}catch(H){D=H;v[2]=D;if($APP.B(v[4]))v[1]=$APP.C(v[4]);else throw D;D=$APP.pB}if(!$APP.Q(D,$APP.pB))return D}}function q(){var v=[null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null];v[0]=u;v[1]=1;return v}
var u=null;u=function(v){switch(arguments.length){case 0:return q.call(this);case 1:return n.call(this,v)}throw Error("Invalid arity: "+arguments.length);};u.xi=q;u.Ea=n;return u}()}(c,function(){return function(k){var l=k[1];if(7===l){var n=k[2];l=$APP.QE(k[7],k[8]);l=$APP.B(l);k[9]=0;k[10]=l;k[11]=n;k[12]=0;k[13]=null;k[2]=null;k[1]=8;return $APP.pB}if(1===l)return k[2]=null,k[1]=2,$APP.pB;if(4===l){var q=$APP.Kd(k[2]);l=$APP.M.ha(q,$APP.WG);var u=$APP.Kd(l);l=$APP.M.ha(u,$APP.zH);n=$APP.M.ha(u,
$APP.JG);var v=$APP.M.ha(u,$APP.SF);u=$APP.M.ha(u,$APP.oF);q=$APP.M.ha(q,$APP.RH);v=$APP.PE(l,v);var A=[$APP.aF,$APP.NF,$APP.rG,$APP.ZH,$APP.JG,$APP.rF,$APP.HH],D=$APP.aF.Ea(q),H=$APP.xM(v),G=$APP.JH(v);n=$APP.Ik(A,[l,D,0,1,n,H,G]);k[14]=u;k[7]=q;k[16]=n;k[15]=l;k[8]=v;k[1]=$APP.p(u)?5:6;return $APP.pB}return 15===l?(k[2]=k[2],k[1]=12,$APP.pB):13===l?(l=k[17],k[1]=$APP.Mc(l)?16:17,$APP.pB):6===l?(n=k[16],k[2]=n,k[1]=7,$APP.pB):17===l?(l=k[17],n=$APP.C(l),n=X0(n),l=$APP.E(l),k[9]=0,k[10]=l,k[18]=n,
k[12]=0,k[13]=null,k[2]=null,k[1]=8,$APP.pB):3===l?$APP.Lv(k,k[2]):12===l?(k[2]=k[2],k[1]=9,$APP.pB):2===l?$APP.Kv(k,4,Y0):11===l?(l=k[10],l=$APP.B(l),k[17]=l,k[1]=l?13:14,$APP.pB):9===l?(n=k[11],u=k[15],l=k[2],n=$APP.RE(n),u=self.postMessage({type:"search-add",payload:W0.write(u)}),k[19]=u,k[20]=l,k[21]=n,k[2]=null,k[1]=2,$APP.pB):5===l?(l=k[14],n=k[16],l=$APP.V.tf(n,$APP.oF,l),k[2]=l,k[1]=7,$APP.pB):14===l?(k[2]=null,k[1]=15,$APP.pB):16===l?(l=k[17],n=$APP.Jb(l),l=$APP.Kb(l),u=$APP.F(n),k[9]=u,
k[10]=l,k[12]=0,k[13]=n,k[2]=null,k[1]=8,$APP.pB):10===l?(n=k[9],l=k[10],u=k[12],q=k[13],v=$APP.Ac(q,u),v=X0(v),k[9]=n,k[10]=l,k[22]=v,k[12]=u+1,k[13]=q,k[2]=null,k[1]=8,$APP.pB):18===l?(k[2]=k[2],k[1]=15,$APP.pB):8===l?(n=k[9],u=k[12],k[1]=$APP.p(u<n)?10:11,$APP.pB):null}}(c,d,e),d,e)}(),g=function(){var k=f();k[6]=d;return k}();return $APP.Hv(g)}}(a,b,12));a+=1}else break})();
self.addEventListener("message",function(a){var b=a.data.type;a=Z0.read(a.data.payload);if($APP.I.ha("process",b))return $APP.Pv(Y0,a);if($APP.I.ha($APP.ks,b))return null;throw Error(["No matching clause: ",$APP.r.Ea(b)].join(""));});
}).call(this);