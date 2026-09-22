"""Shape-preserving cubic motion sampled at 24 FPS for portable GeckoLib playback."""
import math

def smooth_motion(name, length, bones, loop):
    bones={n:list(k) for n,k in bones.items()}
    # Independent follow-through lives on child bones rather than distorting the body.
    def add(bone, channel, points):
        bones.setdefault(bone,[]).extend({'time':t,channel:v} for t,v in points)
    zero=[0,0,0]
    if name=='idle':
        for side,sign in [('left',1),('right',-1)]:
            add(side+'_forearm','rotation',[(0,[2,0,sign]),(1.2,[4,0,sign*2]),(2.6,[1,0,0]),(length,[2,0,sign])])
        add('head','rotation',[(0,zero),(1.5,[1,2,0]),(3,[-1,-1,0]),(length,zero)])
    if name=='walk':
        # Five samples per stride define a bent-knee swing and a planted stance.
        for side,sign in [('left',1),('right',-1)]:
            bones[side+'_shin']=[]
            for i in range(9):
                t=length*i/8; phase=2*math.pi*t/length+(0 if sign==1 else math.pi)
                add(side+'_shin','rotation',[(t,[-28*max(0,math.sin(phase)),0,0])])
            add(side+'_forearm','rotation',[(0,[8,0,0]),(.3,[14,0,0]),(.6,[8,0,0]),(.9,[3,0,0]),(length,[8,0,0])])
        add('chest','rotation',[(0,[2,-3,0]),(.3,[1,0,1]),(.6,[2,3,0]),(.9,[1,0,-1]),(length,[2,-3,0])])
        add('head','rotation',[(0,[-2,3,0]),(.6,[-2,-3,0]),(length,[-2,3,0])])
    if name in ('roar','awaken','enrage'):
        add('left_arm','rotation',[(0,zero),(.3*length,[8,0,12]),(.65*length,[-8,0,18]),(length,zero)])
        add('right_arm','rotation',[(0,zero),(.34*length,[8,0,-12]),(.7*length,[-8,0,-18]),(length,zero)])
        add('left_hand','rotation',[(0,zero),(.45*length,[12,0,5]),(.8*length,[-4,0,0]),(length,zero)])
        add('right_hand','rotation',[(0,zero),(.5*length,[12,0,-5]),(.83*length,[-4,0,0]),(length,zero)])
    if name=='claw_combo':
        for side,delay,sign in [('right',0,-1),('left',.6,1)]:
            add(side+'_forearm','rotation',[(0,zero),(.3+delay,[-28,0,0]),(.57+delay,[12,0,sign*8]),(.83+delay,[-5,0,0]),(length,zero)])
            add(side+'_hand','rotation',[(0,zero),(.35+delay,[-12,0,0]),(.6+delay,[20,0,0]),(.9+delay,[3,0,0]),(length,zero)])
        add('head','rotation',[(0,zero),(.35,[0,12,0]),(.6,[0,-10,0]),(.95,[0,-12,0]),(1.2,[0,10,0]),(length,zero)])
    if name=='ground_slam':
        for side in ['left','right']:
            add(side+'_forearm','rotation',[(0,zero),(.6,[-30,0,0]),(.85,[-40,0,0]),(1.05,[8,0,0]),(1.18,[-6,0,0]),(1.5,[4,0,0]),(length,zero)])
            add(side+'_hand','rotation',[(0,zero),(.8,[-12,0,0]),(1.06,[18,0,0]),(1.3,[5,0,0]),(length,zero)])
        add('body','position',[(0,zero),(.8,[0,1,0]),(1.06,[0,-2,0]),(1.25,[0,-1.5,0]),(length,zero)])
        add('head','rotation',[(0,zero),(.8,[-12,0,0]),(1.12,[12,0,0]),(1.5,[4,0,0]),(length,zero)])
    if name=='core_blast':
        for side,sign in [('left',1),('right',-1)]:
            bones[side+'_arm']=[{'time':t,'rotation':v} for t,v in [(0,zero),(.8,[-10,0,sign*48]),(1.3,[-15,0,sign*55]),(1.5,[5,0,sign*60]),(1.8,[0,0,sign*45]),(length,zero)]]
            add(side+'_forearm','rotation',[(0,zero),(.9,[-18,0,0]),(1.45,[8,0,0]),(1.75,[-4,0,0]),(length,zero)])
        add('head','rotation',[(0,zero),(1.25,[8,0,0]),(1.5,[-8,0,0]),(1.9,[2,0,0]),(length,zero)])
    if name=='death':
        for side,sign in [('left',1),('right',-1)]:
            add(side+'_arm','rotation',[(0,zero),(.8,[10,0,sign*5]),(1.8,[-15,0,sign*10]),(2.65,[12,0,sign*15]),(3,[-4,0,sign*12]),(length,[0,0,sign*12])])
        add('head','rotation',[(0,zero),(1,[14,0,0]),(2,[25,0,5]),(2.7,[8,0,0]),(length,[15,0,0])])
    # Small staggered crown bob: seamless loops, settling envelope on attacks.
    for i in range(5):
        points=[]
        for j in range(9):
            t=length*j/8
            envelope=1 if loop else math.sin(math.pi*t/length)**2
            v=.35*math.sin(2*math.pi*t/length+i*.65)*envelope
            points.append((t,[0,v,0]))
        add(f'crown_shard_{i}','position',points)

    def interpolate(times,values,t):
        count=len(times)
        if count==1: return values[0]
        slopes=[(values[i+1]-values[i])/(times[i+1]-times[i]) for i in range(count-1)]
        tangents=[0.0]*count
        for i in range(1,count-1):
            a,b=slopes[i-1],slopes[i]
            if a*b>0:
                h0,h1=times[i]-times[i-1],times[i+1]-times[i]
                w0,w1=2*h1+h0,h1+2*h0
                tangents[i]=(w0+w1)/(w0/a+w1/b)
        if loop and abs(values[0]-values[-1])<1e-6 and slopes[0]*slopes[-1]>0:
            tangents[0]=tangents[-1]=2/(1/slopes[0]+1/slopes[-1])
        k=min(next((i for i in range(count-1) if t<=times[i+1]),count-2),count-2)
        h=times[k+1]-times[k]; u=max(0,min(1,(t-times[k])/h))
        return (2*u**3-3*u*u+1)*values[k]+(u**3-2*u*u+u)*h*tangents[k]+(-2*u**3+3*u*u)*values[k+1]+(u**3-u*u)*h*tangents[k+1]
    result={}
    for bone,frames in bones.items():
        output=[]
        for channel in ('rotation','position','scale'):
            data={f['time']:f[channel] for f in frames if channel in f}
            if not data: continue
            times=sorted(data)
            samples=sorted(set([round(i/24,6) for i in range(math.floor(length*24)+1)]+times+[length]))
            for t in samples:
                value=[round(interpolate(times,[data[x][axis] for x in times],t),5) for axis in range(3)]
                output.append({'time':t,channel:value})
        result[bone]=output
    return result
