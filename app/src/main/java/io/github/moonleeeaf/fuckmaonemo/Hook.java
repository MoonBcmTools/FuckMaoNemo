package io.github.moonleeeaf.fuckmaonemo;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.loader.AssetsProvider;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Pair;
import android.view.Menu;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Hook implements IXposedHookLoadPackage {
    private static boolean isHooked = false;
    private XSharedPreferences xsp;
    private ClassLoader classLoader;
    private int nohengheng;
    private int aaaa;
    
    public static String MIAO_LIST;
    public static String[][] MIAO;
    
    private XC_MethodHook.Unhook force_set_work_myown_unhook;
    
    private static String[][] demo(String[] array, String split) {
        ArrayList<String[]> ls = new ArrayList<>();
        for (String i : array) {
            String[] b = i.split(split);
            ls.add(new String[] {b[0], b[1]});
        }
        return ls.toArray(new String[][]{});
    }
    
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        xsp = new XSharedPreferences("io.github.moonleeeaf.fuckmaonemo", "config");
        if ("com.codemao.nemo".equals(param.packageName) || xsp.getBoolean("force_enable", false)) {
            XposedBridge.log("[FuckMaoNemo] 开始注入...");
            // 感谢 安宁 提供取加固程序的 ClassLoader 的代码
            XposedBridge.hookAllMethods(
                XposedHelpers.findClass("android.app.ActivityThread", param.classLoader),
                "performLaunchActivity",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mParam) throws Throwable {
                        super.afterHookedMethod(mParam);
                        Object initApp = XposedHelpers.getObjectField(mParam.thisObject, "mInitialApplication");
                        classLoader = initApp.getClass().getClassLoader();
                        hook(param);
                    }
                }
            );
        }
    }
    
    public static Method getMethod(Class clazz, String name, Class<?>... args) throws NoSuchMethodException {
        return clazz.getDeclaredMethod(name, args);
    }
   
    public Application getApplication() throws ClassNotFoundException {
        return (Application) XposedHelpers.callStaticMethod(Class.forName("android.app.ActivityThread"), "currentApplication");
    }
    
    public static Pair<String, String> fuck屏蔽词(String str) {
        String after = str;
        String a = "";
        for (String[] i : MIAO) {
            after = str.replaceAll(i[0], i[1]);
            if (!after.equals(str))
                a += i[0];
            str = after;
        }
        return new Pair<>(str, a);
    }
    
    public void hook(XC_LoadPackage.LoadPackageParam param) throws Exception {
        if (isHooked) return;
        else isHooked = true;
        
        nohengheng = 0;
        aaaa = 0;

        XposedBridge.log("[FuckMaoNemo] 注入中...");
        
        // 劫持设置按钮的点击事件
        load("inject_settings_button", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_注入设置菜单");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.fragment.MineFragment", classLoader),
                    "onClick",
                    View.class
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        View v = (View) mp.args[0];
                        if (v.getId() == 2131297099)
                            v.setId(11451419);
                    }
                    @Override
                    protected void afterHookedMethod(MethodHookParam mp) throws Throwable {
                        View v = (View) mp.args[0];
                        Context c = v.getContext();
                        if (v.getId() == 11451419) {
                            PopupMenu pop = new PopupMenu(c, v);
                            Menu m = pop.getMenu();
                            m.add("Nemo设置").setOnMenuItemClickListener((mm) -> {
                                c.startActivity(new Intent(c, XposedHelpers.findClass("com.codemao.nemo.activity.SettingActivity", classLoader)));
                                return false;
                            });
                            m.add("模块设置").setOnMenuItemClickListener((mm) -> {
                                c.startActivity(new Intent().setComponent(new ComponentName("io.github.moonleeeaf.fuckmaonemo", ConfigActivity.class.getName())));
                                return false;
                            });
                            m.add("打开内置浏览器").setOnMenuItemClickListener((mm) -> {
                                EditText edit = new EditText(c);
                                new AlertDialog.Builder(c)
                                    .setTitle("打开内置浏览器")
                                    .setIcon(android.R.drawable.ic_dialog_info)
                                    .setView(edit)
                                    .setPositiveButton("打开", (d, w) -> {
                                        c.startActivity(new Intent().setData(Uri.parse("nemo://com.codemao.nemo/openwith?type=5&url=" + edit.getText())));
                                    })
                                    .setNegativeButton("取消", (d, w) -> {})
                                    .create()
                                    .show();
                                    
                                return false;
                            });
                            pop.show();
                        
                            mp.setResult(null);
                        }
                    }
                }
            );
        }, true);
        
        // 拦截40x码
        load("fuck_40x", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_拦截40x码");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.retrofit.response.CommonSubcriber", classLoader),
                    "onNext",
                    XposedHelpers.findClass("retrofit2.Response", classLoader)
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mP) throws Throwable {
                        Object res = mP.args[0];
                        int code = (int) XposedHelpers.callMethod(res, "code");
                        if(code >= 400 && code <500) {
                            Object rawRes = XposedHelpers.getObjectField(res, "rawResponse");
                            XposedHelpers.setIntField(rawRes, "code", 200);
                            XposedBridge.log("[FuckMaoNemo] 拦截响应 " + code + " 码");
                            String t = "响应码 " + code;
                            switch (code) {
                                case 401:
                                    t = "已阻止异常登出";
                                    break;
                                case 405:
                                case 422:
                                    t = "API 访问被拒绝，评论区或者已封禁账号？";
                                    break;
                            }
                            Toast.makeText(getApplication(), "[FuckMaoNemo] " + t, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            );
        });
        
        // 劫持分享菜单
        load("long_press_share_work_to_open_more_menu", () -> {
            XposedBridge.log("[FuckMaoNemo] 长按作品分享更多菜单");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.activity.WorkDetailActivity", classLoader),
                    "onCreate",
                    Bundle.class
                ),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mp) throws Throwable {
                        Activity self = (Activity) mp.thisObject;
                        View v = self.findViewById(
                            XposedHelpers.getStaticIntField(
                                XposedHelpers.findClass("com.codemao.nemo.R$id", classLoader),
                                "ll_share"
                            )
                        );
                            
                        long workId = XposedHelpers.getLongField(self, "workId");
                            
                        v.setOnLongClickListener((_v) -> {
                            PopupMenu pop = new PopupMenu(self, v);
                            Menu m = pop.getMenu();
                            m.add("获取分享口令").setOnMenuItemClickListener((mm) -> {
                                openWorkShareCodeGetter(workId);
                                return false;
                            });
                            pop.show();
                            return false;
                        });
                    }
                }
            );
        });
        
        load("test", () -> {
            methodToVoid(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.bean.AuthorInfo", classLoader),
                    "setFork_user",
                    boolean.class
                )
            );
        });
        
        // 绕过防沉迷
        load("fuck_fcm", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_绕过防沉迷");
            methodToVoid(getMethod(
                XposedHelpers.findClass("com.codemao.nemo.activity.WorkDetailActivity", classLoader),
                "checkAntiAddictionState",
                null
            ));
        });
        
         // 最新作品过滤
        load("newest_works_filter", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_最新作品过滤");
                
            final String[] rules = xsp.getString("newest_works_filter_rule_shared", "userId 823651139").split("\n");
            
            final ArrayList<NewestWorksFilter> filters = new ArrayList<NewestWorksFilter>();
                
            for (String s : rules) {
                String type = s.split(" ")[0];
                String value = s.split(" ")[1];
                
                NewestWorksFilter nwf = new NewestWorksFilter(type, value);
                
                filters.add(nwf);
            }
            
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.bean.LatestWorks", classLoader),
                    "getLimit",
                    null //List.class
                ),
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam mp) throws Throwable {
                        return 100;
                    }
                }
            );
                
                
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.bean.LatestWorks", classLoader),
                    "getItems",
                    null //List.class
                ),
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam mp) throws Throwable {
                        List ls = (List) XposedHelpers.getObjectField(mp.thisObject, "items"); // mp.args[0]; // 
                        
                        ArrayList al = new ArrayList();
                            
                        int passed = 0;
                            
                        for (Object o : ls) {
                            String workName = (String) XposedHelpers.getObjectField(o, "work_name");
                            String userId = "" + XposedHelpers.getLongField(o, "user_id");
                            
                            boolean disadd = false;
                                
                            for (NewestWorksFilter filter : filters) {
                                if (filter.matches(userId, workName)) {
                                    disadd = true;
                                    passed++;
                                    break;
                                }
                            }
                                
                            if (disadd) {
                                XposedBridge.log("[FuckMaoNemo] 过滤用户 " + userId + " 的作品 " + workName);
                                
                                continue;
                            }
                            
                            al.add(o);
                        }
                            
                        // XposedHelpers.setObjectField(mp.thisObject, "items", al);
                            
                        int offset = XposedHelpers.getIntField(mp.thisObject, "offset");
                        XposedHelpers.setIntField(mp.thisObject, "offset", offset + passed);
                        
                        int total = XposedHelpers.getIntField(mp.thisObject, "total");
                        // XposedHelpers.setIntField(mp.thisObject, "total", total - passed);
                            
                        XposedBridge.log("[FuckMaoNemo] 过滤完毕 原数据: offset=" + offset + " total=" + total);
                            
                        return al;
                        // return null;
                    }
                }
            );
        });
        
        // 修复KN作品播放
        load("fix_kn_player", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_修复KN作品播放");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.view.X5DWebView", classLoader),
                    "loadUrl",
                    String.class
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        String url = (String) mp.args[0];
                        if (url != null && url.contains("kn.codemao.cn"))
                            url = url.substring(0, url.lastIndexOf("?")) + "&is_nemo_player=true";
                        mp.args[0] = url;
                        XposedBridge.log("KN作品替换链接：" + url);
                    }
                }
            );
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.creativestore.dsbridge.DWebView", classLoader),
                    "loadUrl",
                    String.class
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        String url = (String) mp.args[0];
                        if (url != null && url.contains("kn.codemao.cn"))
                            url =  url.substring(0, url.lastIndexOf("?")) + "&is_nemo_player=true";
                        mp.args[0] = url;
                        XposedBridge.log("KN作品替换链接：" + url);
                    }
                }
            );
        });
        
        // 我就要个人信息换行
        load("remove_edit_desc_filter", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_我就要个人信息换行");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.activity.UserNameDesEditActivity", classLoader),
                    "initView",
                    null
                ),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mp) throws Throwable {
                        EditText e = (EditText) XposedHelpers.getObjectField(mp.thisObject, "editText");
                        e.setFilters(new InputFilter[]{});
                    }
                }
            );
        });
        
        // 作品没有失效
        load("work_is_valid", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_作品没有失效");
            XposedBridge.hookMethod(getMethod(
                XposedHelpers.findClass("com.codemao.nemo.bean.CollectWorkInfo", classLoader),
                "getPublish_time",
                null
            ),new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam mp) throws Throwable {
                    long time = XposedHelpers.getLongField(mp.thisObject, "publish_time");
                    if (time <= 0)
                        time = 114514;
                    return time;
                }
            });
        });
        
        // 岛3我推荐你吗
        load("fuck_box3recommend", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_岛3我推荐你吗");
                
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("retrofit2.Retrofit$1", classLoader),
                    "invoke",
                    new Class[] {
                        Object.class,
                        Method.class,
                        Object[].class
                    }
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        switch (((Method) mp.args[1]).getName()) {
                            case "getRecommendBoxData":
                                mp.setResult(XposedHelpers.callMethod(mp.args[0], "getRecommendPageData", new Object[] {}));
                                break;
                        }
                    }
                }
            );
                
            /*
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.giu.xzz.http.RetrofitManager",classLoader),
                    "create",
                    Class.class
                ),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam mp) throws Throwable {
                        Object s = mp.getResult();
                            
                        XposedBridge.hookAllMethods(
                            s.getClass(),
                            "getRecommendBoxData",
                            new XC_MethodReplacement() {
                                @Override
                                protected Object replaceHookedMethod(MethodHookParam arg0) throws Throwable {
                                    InvocationHandler handler = new InvocationHandler() {
                                        @Override
                                        public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
                                            return null;
                                        }
                                    }
                                    return java.lang.reflect.Proxy.newProxyInstance(
                                        classLoader,
                                        new Class[] { XposedHelpers.findClass("io.reactivex.Observable", classLoader) },
                                        hanlder
                                    );
                                }
                            }
                        );
                    }
                }
            );
            */
        });
        
        // 强制显示再创作按钮
        load("force_show_rework", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_强制显示再创作按钮");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.creativecenter.utils.bcm.bean.CreativeWorkDetailInfo", classLoader),
                    "isFork_enable",
                    null
                ),
                new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam arg0) throws Throwable {
                        return true;
                    }
                }
            );
        });
        
        // 不追踪
        load("no_records", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_不追踪");
            methodToVoid(getMethod(
                XposedHelpers.findClass("cn.codemao.android.stat.CodeMaoStat", classLoader),
                "recordEvent",
                String.class,
                Map.class
            ));
        });
        
        // 反防抓包
        load("fuck_no_proxy", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_反防抓包");
            // TODO：其实可以从 OkHttp 底层去Hook的
            methodToVoid(
                getMethod(
                    XposedHelpers.findClass("okhttp3.OkHttpClient$Builder", classLoader),
                    "proxy",
                    Proxy.class
                )
            );
        });
        
        // 屏蔽更新
        load("fuck_update", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_屏蔽更新");
            methodToVoid(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.sdk.update.NetChangeReceiver", classLoader),
                    "onReceive",
                    Context.class,
                    Intent.class
                )
            );
        });
        
        // 强制置顶评论
        load("force_top_comment", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_强制置顶评论");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.view.CommentOptionDialogV2", classLoader),
                    "checkIsSelf",
                    null
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        XposedHelpers.setBooleanField(mp.thisObject, "isOwnWork", true);
                    }
                }
            );
        });
        
        // 谋权篡位
        load("force_set_work_myown", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_谋权篡位");
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.activity.WorkDetailActivity", classLoader),
                    "setWorkDetailData",
                    null
                ),
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                        force_set_work_myown_unhook = XposedBridge.hookMethod(
                        getMethod(
                            XposedHelpers.findClass("com.codemao.creativecenter.utils.bcm.bean.AuthorInfo", classLoader),
                            "getId",
                            null
                        ),
                        new XC_MethodReplacement() {
                            @Override
                            protected Object replaceHookedMethod(MethodHookParam hp) throws Throwable {
                                Method m = getMethod(XposedHelpers.findClass("com.codemao.nemo.util.LocalUserHelper", classLoader), "getUserInfo", null);
                                m.setAccessible(true);
                                Object usrInfo = m.invoke(null, null);
                                        
                                m = getMethod(usrInfo.getClass(), "getId", null);
                                m.setAccessible(true);
                                        
                                force_set_work_myown_unhook.unhook(); // 希望人没事
                                return m.invoke(usrInfo, null); // long 类型
                            }
                        });
                    }
                }
            );
        });
        
        // 防止屏蔽屏蔽词
        load("fuck_miao", () -> {
            XposedBridge.log("[FuckMaoNemo] Hook_反屏蔽");
                
            MIAO_LIST = xsp.getString("MIAO_LIST_SHARED", null);
                
            MIAO = demo(MIAO_LIST.split("\n"), " ");
                
            XC_MethodHook hook = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam mp) throws Throwable {
                    Pair<String, String> sb = fuck屏蔽词((String) XposedHelpers.getObjectField(mp.thisObject, "content"));
                    XposedHelpers.setObjectField(mp.thisObject, "content", sb.first);
                    if (!"".equals(sb.second))
                        Toast.makeText(getApplication(), "[FuckMaoNemo] 发送内容已尝试防止屏蔽下列字符词语:" + sb.second, Toast.LENGTH_LONG).show();
                }
            };
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.fragment.WorkCommentFragment", classLoader),
                    "sendReply",
                    null
                ),
                hook
            );
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.fragment.WorkCommentFragment", classLoader),
                    "sendComment",
                    null
                ),
                hook
            );
            XposedBridge.hookMethod(
                getMethod(
                    XposedHelpers.findClass("com.codemao.nemo.activity.CommentDetailActivity", classLoader),
                    "send",
                    null
                ),
                hook
            );
        });
        
        XposedBridge.log("[FuckMaoNemo] 执行完毕");
        
        Toast.makeText(getApplication(), "[FuckMaoNemo] 加载成功 (≧▽≦)\n" + nohengheng + " 个功能加载成功, " + aaaa + " 个失败", Toast.LENGTH_LONG).show();
    }
    
    public interface Callback {
        public void onCallback() throws Exception;
    }
   
    public void methodToVoid(Method m) {
        XposedBridge.hookMethod(m, new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam arg0) throws Throwable {
                return null;
            }
        });
    }
    
    public void load(String pref, Callback cb) {
        load(pref, cb ,false);
    }
    
    public void load(String pref, Callback cb, boolean def) {
        if (xsp.getBoolean(pref, def)) {
            try {
                cb.onCallback();
                nohengheng++;
            } catch (Exception e) {
                XposedBridge.log(e);
                aaaa++;
            }
        }
    }
    
    public void openWorkShareCodeGetter(long workId) {
        try {
            Context self = getApplication();
            String cookie = CookieManager.getInstance().getCookie("https://api.codemao.cn");
            int a = cookie.indexOf(";", cookie.indexOf("Bearer "));
            String token = cookie.substring(cookie.indexOf("Bearer ") + "Bearer ".length(), a == -1 ? cookie.length() : a - 1);
                                     
            // XposedBridge.log("cookie=" + cookie);
            // XposedBridge.log("token=" + token);
            self.startActivity(new Intent().setFlags(Intent.FLAG_ACTIVITY_NEW_TASK).setData(Uri.parse("nemo://com.codemao.nemo/openwith?type=5&url=" + 
                URLEncoder.encode("https://moonbcmtools.github.io/CodemaoNemoOneKeyBuildShareCode/?token=" + token + "&workId=" + workId, "utf-8")
            )));
        } catch(Exception e) {
            XposedBridge.log(e);
            // Toast.makeText(self, "[FuckMaoNemo] " + e, Toast.LENGTH_LONG).show();
        }
    }
    
    public static class NewestWorksFilter {
        private String type;
        private String value;
        
        public NewestWorksFilter(String type, String value) {
            this.type = type;
            this.value = value;
        }
        
        public boolean matches(String userId, String workName) {
            if ("workName".equals(this.type)) {
                return Pattern.matches(value, workName);
            } else if ("userId".equals(this.type)) {
                return userId.equals(value);
            }
            
            return false;
        }
        
    }
    
}
