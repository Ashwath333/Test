package eu.chainfire.libsuperuser;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

public class Application extends android.app.Application {
    private static Handler mApplicationHandler;

    /* renamed from: eu.chainfire.libsuperuser.Application.1 */
    static class C00261 implements Runnable {
        final /* synthetic */ Context val$c;
        final /* synthetic */ String val$m;

        C00261(Context context, String str) {
            this.val$c = context;
            this.val$m = str;
        }

        public void run() {
            Toast.makeText(this.val$c, this.val$m, 1).show();
        }
    }

    public static void toast(Context context, String message) {
        if (context != null) {
            if (!(context instanceof Application)) {
                context = context.getApplicationContext();
            }
            if (context instanceof Application) {
                ((Application) context).runInApplicationThread(new C00261(context, message));
            }
        }
    }

    static {
        mApplicationHandler = new Handler();
    }

    public void runInApplicationThread(Runnable r) {
        mApplicationHandler.post(r);
    }

    public void onCreate() {
        super.onCreate();
        try {
            Class.forName("android.os.AsyncTask");
        } catch (ClassNotFoundException e) {
        }
    }
}
