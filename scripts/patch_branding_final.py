from pathlib import Path

JAVA=Path('app/src/main/java/com/rgapro1/ocaso/MainActivity.java')
s=JAVA.read_text(encoding='utf-8')

if 'finalBrandingApplied' not in s:
    helper='''
    // Final brand layer: the RgaPro logo must remain visible after every UI patch.
    private final boolean finalBrandingApplied = true;
    private View finalBrandingSurface(View page){
        FrameLayout frame=new FrameLayout(this);
        frame.setBackgroundColor(BG);
        frame.addView(page,new FrameLayout.LayoutParams(-1,-1));
        ImageView mark=new ImageView(this);
        mark.setImageResource(R.drawable.ic_rgapro);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        mark.setAlpha(0.055f);
        mark.setClickable(false); mark.setFocusable(false);
        FrameLayout.LayoutParams wm=new FrameLayout.LayoutParams(dp(620),dp(620),Gravity.CENTER);
        frame.addView(mark,wm);
        return frame;
    }
    private LinearLayout finalBrandRow(boolean dark){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark=new ImageView(this); mark.setImageResource(R.drawable.ic_rgapro);
        mark.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        row.addView(mark,new LinearLayout.LayoutParams(dp(52),dp(52)));
        LinearLayout texts=col();
        texts.addView(tv("RgaPro",24,dark?Color.WHITE:NAVY,true));
        texts.addView(tv("MI CARTERA OCASO",11,dark?Color.WHITE:MUTED,true));
        row.addView(texts,new LinearLayout.LayoutParams(0,dp(56),1));
        return row;
    }
'''
    marker='    @Override public void onCreate(Bundle b)'
    if marker not in s: raise SystemExit('MainActivity insertion marker not found')
    s=s.replace(marker,helper+'\n'+marker,1)


def method_bounds(src, signature):
    start=src.find(signature)
    if start<0:return None
    brace=src.find('{',start)
    depth=0
    for i in range(brace,len(src)):
        if src[i]=='{':depth+=1
        elif src[i]=='}':
            depth-=1
            if depth==0:return start,i+1
    raise SystemExit('Unbalanced method: '+signature)

# Create/login: guarantee a logo even if an earlier patch already changed the layout.
for signature in ('    private void createUser(){','    private void showLogin(){'):
    bounds=method_bounds(s,signature)
    if not bounds: continue
    start,end=bounds;m=s[start:end]
    if 'setImageResource(R.drawable.ic_rgapro)' not in m:
        marker='LinearLayout l=col();'
        logo='LinearLayout l=col();ImageView accessLogo=new ImageView(this);accessLogo.setImageResource(R.drawable.ic_rgapro);accessLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);l.addView(accessLogo,new LinearLayout.LayoutParams(dp(132),dp(132)));'
        if marker not in m: raise SystemExit('Login/create layout marker not found')
        m=m.replace(marker,logo,1)
    m=m.replace('setContentView(l);','setContentView(finalBrandingSurface(l));')
    m=m.replace('setContentView(withWatermark(l));','setContentView(finalBrandingSurface(l));')
    s=s[:start]+m+s[end:]

# Dashboard: force a final header logo regardless of the previous branding helper.
bounds=method_bounds(s,'    private void home(){')
if bounds:
    start,end=bounds;m=s[start:end]
    if 'top.addView(finalBrandRow(true)' not in m:
        if 'top.addView(brandHeader(),' in m:
            m=m.replace('top.addView(brandHeader(),new LinearLayout.LayoutParams(-1,dp(48)));','top.addView(finalBrandRow(true),new LinearLayout.LayoutParams(-1,dp(58)));',1)
        else:
            old='top.addView(tv("RgaPro",26,Color.WHITE,true));'
            if old in m:m=m.replace(old,'top.addView(finalBrandRow(true),new LinearLayout.LayoutParams(-1,dp(58)));',1)
    if 'finalBrandRow(false)' not in m:
        menu='side.addView(tv("MENÚ",13,MUTED,true),new LinearLayout.LayoutParams(-1,dp(38)));'
        if menu in m:m=m.replace(menu,'side.addView(finalBrandRow(false),new LinearLayout.LayoutParams(-1,dp(70))); side.addView(tv("MENÚ",13,MUTED,true),new LinearLayout.LayoutParams(-1,dp(32)));',1)
    m=m.replace('setContentView(root);','setContentView(finalBrandingSurface(root));')
    m=m.replace('setContentView(withWatermark(root));','setContentView(finalBrandingSurface(root));')
    s=s[:start]+m+s[end:]

# Internal pages: add the logo to the title bar when not already present.
bounds=method_bounds(s,'    private void page(String title,String sub){')
if bounds:
    start,end=bounds;m=s[start:end]
    if 'pageLogo.setImageResource(R.drawable.ic_rgapro)' not in m:
        needle='h.addView(tv(title,20,Color.WHITE,true));h.addView(tv(sub,12,Color.WHITE,false));'
        repl='ImageView pageLogo=new ImageView(this);pageLogo.setImageResource(R.drawable.ic_rgapro);pageLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);h.addView(pageLogo,new LinearLayout.LayoutParams(dp(46),dp(46)));LinearLayout pageTitles=col();pageTitles.addView(tv(title,20,Color.WHITE,true));pageTitles.addView(tv(sub,12,Color.WHITE,false));h.addView(pageTitles,new LinearLayout.LayoutParams(0,dp(56),1));'
        if needle in m:m=m.replace(needle,repl,1)
    s=s[:start]+m+s[end:]

JAVA.write_text(s,encoding='utf-8')
print('Final RgaPro branding applied after all UI patches')
