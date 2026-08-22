from pathlib import Path

JAVA=Path("app/src/main/java/com/rgapro1/ocaso/MainActivity.java")
s=JAVA.read_text(encoding="utf-8")

if "finalBrandingApplied" in s:
    print("Final branding already applied")
    raise SystemExit(0)

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
if 'private View finalBrandingSurface' not in s:
    if marker not in s: raise SystemExit('MainActivity insertion marker not found')
    s=s.replace(marker,helper+'\n'+marker,1)

# Login/create screens: show the actual RgaPro mark, not only the text name.
login_head='LinearLayout l=col();l.setBackgroundColor(BG);l.setPadding(dp(24),dp(20),dp(24),dp(24));l.setGravity(Gravity.CENTER_HORIZONTAL);l.addView(tv("RgaPro",32,NAVY,true));'
login_head_new='LinearLayout l=col();l.setBackgroundColor(BG);l.setPadding(dp(24),dp(20),dp(24),dp(24));l.setGravity(Gravity.CENTER_HORIZONTAL);ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.ic_rgapro);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);l.addView(logo,new LinearLayout.LayoutParams(dp(132),dp(132)));l.addView(tv("RgaPro",32,NAVY,true));'
s=s.replace(login_head,login_head_new,2)

# Dashboard header and side menu.
s=s.replace('top.addView(tv("RgaPro",26,Color.WHITE,true)); top.addView(tv("Panel principal · "+currentUser,15,Color.WHITE,false));','top.addView(finalBrandRow(true),new LinearLayout.LayoutParams(-1,dp(58))); top.addView(tv("Panel principal · "+currentUser,15,Color.WHITE,false));',1)
s=s.replace('side.addView(tv("MENÚ",13,MUTED,true),new LinearLayout.LayoutParams(-1,dp(38)));','side.addView(finalBrandRow(false),new LinearLayout.LayoutParams(-1,dp(70))); side.addView(tv("MENÚ",13,MUTED,true),new LinearLayout.LayoutParams(-1,dp(32)));',1)

# Page headers: keep the logo visible on client/policy/document screens too.
s=s.replace('h.addView(tv(title,20,Color.WHITE,true));h.addView(tv(sub,12,Color.WHITE,false));','ImageView pageLogo=new ImageView(this);pageLogo.setImageResource(R.drawable.ic_rgapro);pageLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);h.addView(pageLogo,new LinearLayout.LayoutParams(dp(46),dp(46)));LinearLayout pageTitles=col();pageTitles.addView(tv(title,20,Color.WHITE,true));pageTitles.addView(tv(sub,12,Color.WHITE,false));h.addView(pageTitles,new LinearLayout.LayoutParams(0,dp(56),1));',1)

# Wrap entry screens with a non-interactive watermark layer. Applied last so later patches cannot hide it.
s=s.replace('setContentView(l);}', 'setContentView(finalBrandingSurface(l));}',2)
s=s.replace('setContentView(root);\n    }', 'setContentView(finalBrandingSurface(root));\n    }',1)

JAVA.write_text(s,encoding='utf-8')
print('Final RgaPro branding applied after all UI patches')
