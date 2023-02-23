package com.gg.hookservice;

import android.os.IBinder;
import android.os.IInterface;
import android.text.TextUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @description:
 * @author: GG
 * @createDate: 2023 2.23 0023 16:38
 * @updateUser:
 * @updateDate: 2023 2.23 0023 16:38
 */
public class SystemServiceBinderHooker {

    private String mServiceName;
    private String mServiceClassName;

    private HookCallback mHookCallback;

    public interface HookCallback {
        void onServiceMethodInvoke(Method method, Object[] args);

        Object onServiceMethodIntercept(Object receiver, Method method, Object[] args);
    }

    public SystemServiceBinderHooker(String serviceName, String serviceClassName, HookCallback hookCallback) {
        this.mServiceClassName = serviceClassName;
        this.mServiceName = serviceName;
        this.mHookCallback = hookCallback;
    }

    public boolean hook() {
        try {
            // 3.1. 获取 IBinder 对象 hook 住后塞到 ServiceManager 的 sCache 中
            // 3.1.1 从 ServiceManager 中先获取到 IBinder 对象
            Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
            Method getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String.class);
            getServiceMethod.setAccessible(true);
            final IBinder serviceBinder = (IBinder) getServiceMethod.invoke(null, mServiceName);

            // 3.2.2 动态代理 hook 住
            IBinder proxyServiceBinder = (IBinder) Proxy.newProxyInstance(serviceManagerClass.getClassLoader(), new Class[]{IBinder.class},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                            // 3.2. 处理 IBinder hook 对象的 queryLocalInterface 方法，返回一个 IWifiManager.Stub.Proxy(obj) 的 hook 对象
                            if (TextUtils.equals(method.getName(), "queryLocalInterface")) {
                                return createServiceProxy(serviceBinder);
                            }
                            return method.invoke(serviceBinder, objects);
                        }
                    });
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Object createServiceProxy(IBinder serviceBinder) {
        // 3.2. 处理 IBinder hook 对象的 queryLocalInterface 方法，返回一个 IWifiManager.Stub.Proxy(obj) 的 hook 对象
        // 3.2.1 创建 Proxy 对象 new IWifiManager.Stub.Proxy(obj);
        try {
            Class<?> serviceProxyClass = Class.forName(mServiceClassName + "$Stub$Proxy");
            Constructor<?> serviceProxyConstructor = serviceProxyClass.getDeclaredConstructor(IBinder.class);
            serviceProxyConstructor.setAccessible(true);
            final Object originServiceProxy = serviceProxyConstructor.newInstance(serviceBinder);
            // 3.2.2 hook 3.2.1 创建的对象
            IBinder serviceProxyHooker = (IBinder) Proxy.newProxyInstance(serviceProxyClass.getClassLoader(), new Class[]{IBinder.class,
                    IInterface.class, Class.forName(mServiceClassName)}, new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
                    if (mHookCallback != null) {
                        mHookCallback.onServiceMethodInvoke(method, objects);
                        Object result = mHookCallback.onServiceMethodIntercept(originServiceProxy, method, objects);
                        if (result != null) {
                            return result;
                        }
                    }
                    return method.invoke(originServiceProxy, objects);
                }
            });
            return serviceProxyHooker;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
