// Turn the @formatter:off so we don't change the 3rd party source layout

package org.tvrenamer.view;

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.C;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Provide a hook to connecting the Preferences, About and Quit menu items of
 * the Mac OS X Application menu when using the SWT Cocoa bindings.<p>
 *
 * This code does not require the Cocoa SWT JAR in order to be compiled as it
 * uses reflection to access the Cocoa specific API methods.  In its original
 * version, it depended on JFace (for IAction), but it has been modified to use
 * SWT Listeners instead, since this is an SWT-only application.<p>
 *
 * This code was influenced by the CarbonUIEnhancer from Agynami [note, link
 * no longer functional] with the implementation being modified from the
 * org.eclipse.ui.internal.cocoa.CocoaUIEnhancer [link also broken].<p>
 *
 * This code was derived from
 * <a href="http://www.transparentech.com/opensource/cocoauienhancer">
 * TransparenTech Cocoa UI Enhancer</a><p>
 *
 * This class works with both the 32-bit and 64-bit versions of the SWT Cocoa
 * bindings.  <p>
 *
 * This class is released under the Eclipse Public License
 * (<a href="http://www.eclipse.org/legal/epl-v10.html">EPL</a>).
 */
class CocoaUIEnhancer {
    private static final String SHARED_APPLICATION = "sharedApplication";

    private static final long kAboutMenuItem = 0;
    private static final long kPreferencesMenuItem = 2;
    private static final long kQuitMenuItem = 10;

    private static long sel_preferencesMenuItemSelected_;
    private static long sel_aboutMenuItemSelected_;
    private static Callback proc3Args;

    private final String appName;

    /**
     * Class invoked via the Callback object to run the about and preferences actions.
     */
    private static class MenuHookObject {
        final Listener about;
        final Listener pref;

        public MenuHookObject(Listener about, Listener pref) {
            this.about = about;
            this.pref = pref;
        }

        /**
         * Will be called on 32bit SWT.
         *
         * @param id
         *    not used
         * @param sel
         *    the selection
         * @param arg0
         *    not used
         * @return an irrelevant value; ignore it
         */
        public int actionProc(int id, int sel, int arg0) {
            return (int) actionProc((long) id, (long) sel, (long) arg0);
        }

        /**
         * Will be called on 64bit SWT.
         *
         * @param id
         *    not used
         * @param sel
         *    the selection
         * @param arg0
         *    not used
         * @return an irrelevant value; ignore it
         */
        @SuppressWarnings({"UnusedParameters", "SameReturnValue"})
        public long actionProc(long id, long sel, long arg0) {
            if (sel == sel_aboutMenuItemSelected_) {
                about.handleEvent(null);
            } else if (sel == sel_preferencesMenuItemSelected_) {
                pref.handleEvent(null);
            }
            // else Unknown selection!
            // Return value is not used.
            return 99;
        }
    }

    /**
     * Construct a new CocoaUIEnhancer.
     *
     * @param appName
     *            The name of the application. It will be used to customize the
     *            About and Quit menu items. If you do not wish to customize the
     *            About and Quit menu items, just pass <tt>null</tt> here.
     */
    @SuppressWarnings("SameParameterValue")
    public CocoaUIEnhancer(String appName) {
        this.appName = appName;
    }

    /**
     * Hook the given Listener to the Mac OS X application Quit menu and the
     * IActions to the About and Preferences menus.
     *
     * @param display
     *            The Display to use.
     * @param quitListener
     *            The listener to invoke when the Quit menu is invoked.
     * @param aboutAction
     *            The action to run when the About menu is invoked.
     * @param preferencesAction
     *            The action to run when the Preferences menu is invoked.
     */
    public void hookApplicationMenu(Display display, Listener quitListener, Listener aboutAction,
                                     Listener preferencesAction)
    {
        // This is our callbackObject whose 'actionProc' method will be called
        // when the About or Preferences menuItem is invoked.
        MenuHookObject target = new MenuHookObject(aboutAction, preferencesAction);

        try {
            // Initialize the menuItems.
            initialize(target);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        // Connect the quit/exit menu.
        if (!display.isDisposed()) {
            display.addListener(SWT.Close, quitListener);
        }

        // Schedule disposal of callback object
        //noinspection Convert2Lambda
        display.disposeExec(new Runnable() {
            @Override
            public void run() {
                invoke(proc3Args, "dispose");
            }
        });
    }

    private void initialize(Object callbackObject)
            throws Exception
    {
        Class<?> osCls = classForName("org.eclipse.swt.internal.cocoa.OS");

        // Register names in objective-c.
        sel_preferencesMenuItemSelected_ = registerName(osCls, "preferencesMenuItemSelected:"); //$NON-NLS-1$
        sel_aboutMenuItemSelected_ = registerName(osCls, "aboutMenuItemSelected:"); //$NON-NLS-1$

        // Create an SWT Callback object that will invoke the actionProc method
        // of our internal callbackObject.
        proc3Args = new Callback(callbackObject, "actionProc", 3); //$NON-NLS-1$
        Method getAddress = Callback.class.getMethod("getAddress");
        Object object = getAddress.invoke(proc3Args, (Object[]) null);
        long proc3 = convertToLong(object);
        if (proc3 == 0) {
            SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
        }

        Class<?> nsmenuCls = classForName("org.eclipse.swt.internal.cocoa.NSMenu");
        Class<?> nsmenuitemCls = classForName("org.eclipse.swt.internal.cocoa.NSMenuItem");
        Class<?> nsstringCls = classForName("org.eclipse.swt.internal.cocoa.NSString");
        Class<?> nsapplicationCls = classForName("org.eclipse.swt.internal.cocoa.NSApplication");

        // Instead of creating a new delegate class in objective-c, just use the
        // current SWTApplicationDelegate. An instance of this is a field of the
        // Cocoa Display object and is already the target for the menuItems. So
        // just get this class and add the new methods to it.
        object = invoke(osCls, "objc_lookUpClass", new Object[] { "SWTApplicationDelegate" });
        long cls = convertToLong(object);

        // Add the action callbacks for Preferences and About menu items.
        invoke(osCls, "class_addMethod", new Object[] {
                wrapPointer(cls),
                wrapPointer(sel_preferencesMenuItemSelected_),
                wrapPointer(proc3),
                "@:@" }); //$NON-NLS-1$
        invoke(osCls, "class_addMethod", new Object[] {
                wrapPointer(cls),
                wrapPointer(sel_aboutMenuItemSelected_),
                wrapPointer(proc3),
                "@:@" }); //$NON-NLS-1$

        // Get the Mac OS X Application menu.
        Object sharedApplication = invokeSharedApplication(nsapplicationCls);
        Object mainMenu = invoke(sharedApplication, "mainMenu");
        Object mainMenuItem = invoke(nsmenuCls, mainMenu, "itemAtIndex", new Object[] { wrapPointer(0) });
        Object appMenu = invoke(mainMenuItem, "submenu");

        // Create the About <application-name> menu command
        Object aboutMenuItem =
            invoke(nsmenuCls, appMenu, "itemAtIndex", new Object[] { wrapPointer(kAboutMenuItem) });
        if (appName != null) {
            Object nsStr = invoke(nsstringCls, "stringWith", new Object[] { "About " + appName });
            invoke(nsmenuitemCls, aboutMenuItem, "setTitle", new Object[] { nsStr });
        }
        // Rename the quit action.
        if (appName != null) {
            Object quitMenuItem =
                invoke(nsmenuCls, appMenu, "itemAtIndex", new Object[] { wrapPointer(kQuitMenuItem) });
            Object nsStr = invoke(nsstringCls, "stringWith", new Object[] { "Quit " + appName });
            invoke(nsmenuitemCls, quitMenuItem, "setTitle", new Object[] { nsStr });
        }

        // Enable the Preferences menuItem.
        Object prefMenuItem =
            invoke(nsmenuCls, appMenu, "itemAtIndex", new Object[] { wrapPointer(kPreferencesMenuItem) });
        invoke(nsmenuitemCls, prefMenuItem, "setEnabled", new Object[] { true });

        // Set the action to execute when the About or Preferences menuItem is
        // invoked.
        //
        // We don't need to set the target here as the current target is the
        // SWTApplicationDelegate and we have registered the new selectors on
        // it. So just set the new action to invoke the selector.
        invoke(nsmenuitemCls, prefMenuItem, "setAction",
                new Object[] { wrapPointer(sel_preferencesMenuItemSelected_) });
        invoke(nsmenuitemCls, aboutMenuItem, "setAction",
                new Object[] { wrapPointer(sel_aboutMenuItemSelected_) });
    }

    private long registerName(Class<?> osCls, String name)
            throws IllegalArgumentException, SecurityException
    {
        Object object = invoke(osCls, "sel_registerName", new Object[] { name });
        return convertToLong(object);
    }

    private long convertToLong(Object object) {
        if (object instanceof Integer) {
            Integer i = (Integer) object;
            return i.longValue();
        }
        if (object instanceof Long) {
            return (Long) object;
        }
        return 0;
    }

    @SuppressWarnings("UnnecessaryBoxing")
    private static Object wrapPointer(long value) {
        if (C.PTR_SIZEOF == 8) {
            return Long.valueOf(value);
        } else {
            return Integer.valueOf((int) value);
        }
    }

    private static Object invoke(Class<?> clazz, String methodName, Object[] args) {
        return invoke(clazz, null, methodName, args);
    }

    private static Object invoke(Class<?> clazz, Object target, String methodName, Object[] args) {
        try {
            Class<?>[] signature = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                Class<?> thisClass = args[i].getClass();
                if (thisClass == Integer.class) {
                    signature[i] = int.class;
                } else if (thisClass == Long.class) {
                    signature[i] = long.class;
                } else if (thisClass == Byte.class) {
                    signature[i] = byte.class;
                } else if (thisClass == Boolean.class) {
                    signature[i] = boolean.class;
                } else {
                    signature[i] = thisClass;
                }
            }
            Method method = clazz.getMethod(methodName, signature);
            return method.invoke(target, args);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException  e) {
            throw new IllegalStateException(e);
        }
    }

    private Class<?> classForName(String classname) {
        try {
            return Class.forName(classname);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private Object invokeSharedApplication(Class<?> cls) {
        return invoke(cls, (Class<?>[]) null, (Object[]) null);
    }

    private Object invoke(Class<?> cls, Class<?>[] paramTypes, Object... arguments) {
        try {
            Method m = cls.getDeclaredMethod(SHARED_APPLICATION, paramTypes);
            return m.invoke(null, arguments);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Object invoke(Object obj, String methodName) {
        return invoke(obj, methodName, null, (Object[]) null);
    }

    @SuppressWarnings("SameParameterValue")
    private Object invoke(Object obj, String methodName, Class<?>[] paramTypes, Object... arguments) {
        try {
            Method m = obj.getClass().getDeclaredMethod(methodName, paramTypes);
            return m.invoke(obj, arguments);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
