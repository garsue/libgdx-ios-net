package fm.garsue.ios.net;

import com.badlogic.gdx.Gdx;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.uikit.UIApplication;

import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.backends.iosrobovm.ObjCNet;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;

public class IOSLauncher extends IOSApplication.Delegate {

    private ObjCNet objCNet;

    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        final IOSApplication app = new IOSApplication(new IOSNetGame(), config);
        objCNet = new ObjCNet(app);
        return app;
    }

    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        boolean ret = super.didFinishLaunching(application, launchOptions);
        Gdx.net = objCNet;
        return ret;
    }

    public static void main(String[] argv) {
        try (NSAutoreleasePool pool = new NSAutoreleasePool()) {
            UIApplication.main(argv, null, IOSLauncher.class);
        }
    }
}
