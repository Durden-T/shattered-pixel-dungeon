/*
 * The backend initialization is adapted from libGDX's IOSApplication.
 * Copyright 2011 See https://github.com/libgdx/libgdx/blob/1.13.1/AUTHORS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.badlogic.gdx.backends.iosrobovm;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationDelegateAdapter;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UICoordinateSpace;
import org.robovm.apple.uikit.UIInterfaceOrientation;
import org.robovm.apple.uikit.UIScene;
import org.robovm.apple.uikit.UISceneConfiguration;
import org.robovm.apple.uikit.UISceneConnectionOptions;
import org.robovm.apple.uikit.UISceneSession;
import org.robovm.apple.uikit.UISceneSessionRole;
import org.robovm.apple.uikit.UITraitCollection;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.apple.uikit.UIWindowScene;
import org.robovm.apple.uikit.UIWindowSceneDelegateAdapter;
import org.robovm.apple.uikit.UIWindowSceneGeometry;
import org.robovm.objc.annotation.Method;

public class SceneIOSApplication extends IOSApplication {

	public static abstract class Delegate extends UIApplicationDelegateAdapter {
		private SceneIOSApplication app;

		protected abstract SceneIOSApplication createApplication();

		@Override
		public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions options) {
			application.addStrongRef(this);
			return true;
		}

		@Override
		public UISceneConfiguration getConfigurationForConnectingSceneSession(UIApplication application,
				UISceneSession session, UISceneConnectionOptions options) {
			if (session.getRole() != UISceneSessionRole.Application) {
				return null;
			}
			UISceneConfiguration configuration = new UISceneConfiguration(null, session.getRole());
			configuration.setDelegateClass(SceneDelegate.class);
			return configuration;
		}

		@Override
		public void willTerminate(UIApplication application) {
			if (app != null) {
				app.willTerminate(application);
			}
		}
	}

	public static class SceneDelegate extends UIWindowSceneDelegateAdapter {
		private SceneIOSApplication app;

		@Override
		public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions options) {
			UIApplication application = UIApplication.getSharedApplication();
			Delegate delegate = (Delegate) application.getDelegate();
			if (delegate.app == null) {
				delegate.app = delegate.createApplication();
				delegate.app.connect(application, (UIWindowScene) scene);
			} else {
				delegate.app.uiWindow.setWindowScene((UIWindowScene) scene);
				delegate.app.uiWindow.makeKeyAndVisible();
			}
			app = delegate.app;
			scene.addStrongRef(this);
		}

		@Override
		public UIWindow getWindow() {
			return app == null ? null : app.uiWindow;
		}

		@Override
		public void sceneDidBecomeActive(UIScene scene) {
			app.didBecomeActive(app.uiApp);
		}

		@Override
		public void sceneWillResignActive(UIScene scene) {
			app.willResignActive(app.uiApp);
		}

		@Override
		public void sceneWillEnterForeground(UIScene scene) {
			app.willEnterForeground(app.uiApp);
		}

		@Override
		public void didUpdateCoordinateSpace(UIWindowScene scene, UICoordinateSpace previousSpace,
				UIInterfaceOrientation previousOrientation, UITraitCollection previousTraits) {
			sceneGeometryChanged();
		}

		@Method(selector = "windowScene:didUpdateEffectiveGeometry:")
		public void didUpdateEffectiveGeometry(UIWindowScene scene, UIWindowSceneGeometry previousGeometry) {
			sceneGeometryChanged();
		}

		private void sceneGeometryChanged() {
			if (app != null) {
				app.getUIViewController().getView().setNeedsLayout();
			}
		}
	}

	private IOSScreenBounds bounds;

	public SceneIOSApplication(ApplicationListener listener, IOSApplicationConfiguration config) {
		super(listener, config);
	}

	// Initialize the pinned libGDX backend once UIKit has connected its window scene.
	private void connect(UIApplication application, UIWindowScene scene) {
		setApplicationLogger(new IOSApplicationLogger());
		Gdx.app = this;
		uiApp = application;
		uiApp.setIdleTimerDisabled(config.preventScreenDimming);
		uiWindow = new UIWindow(scene);
		input = createInput();
		graphics = createGraphics();
		files = createFiles();
		audio = createAudio(config);
		net = new IOSNet(this, config);
		Gdx.gl = Gdx.gl20 = graphics.gl20;
		Gdx.gl30 = graphics.gl30;
		Gdx.graphics = graphics;
		Gdx.files = files;
		Gdx.audio = audio;
		Gdx.input = input;
		Gdx.net = net;
		input.setupPeripherals();
		uiWindow.setRootViewController(graphics.viewController);
		uiWindow.makeKeyAndVisible();
		graphics.updateSafeInsets();
		graphics.view.display();
		listener.create();
		listener.resize(graphics.getWidth(), graphics.getHeight());
		graphics.view.display();
	}

	@Override
	protected IOSScreenBounds computeBounds() {
		CGRect frame = uiWindow.getBounds();
		pixelsPerPoint = (float) uiWindow.getWindowScene().getScreen().getNativeScale();
		return bounds = new IOSScreenBounds(0, 0, (int) Math.round(frame.getWidth()), (int) Math.round(frame.getHeight()),
				(int) Math.round(frame.getWidth() * pixelsPerPoint), (int) Math.round(frame.getHeight() * pixelsPerPoint));
	}

	@Override
	public IOSScreenBounds getScreenBounds() {
		return bounds;
	}

	protected void displayInsetsChanged() {
	}

	@Override
	protected IOSUIViewController createUIViewController(IOSGraphics graphics) {
		return new IOSUIViewController(this, graphics) {
			private UIInterfaceOrientation orientation = UIInterfaceOrientation.Unknown;

			@Override
			public void viewSafeAreaInsetsDidChange() {
				super.viewSafeAreaInsetsDidChange();
				getView().setNeedsLayout();
			}

			@Override
			public void viewDidLayoutSubviews() {
				IOSScreenBounds previous = graphics.screenBounds;
				int left = graphics.getSafeInsetLeft();
				int top = graphics.getSafeInsetTop();
				int right = graphics.getSafeInsetRight();
				int bottom = graphics.getSafeInsetBottom();
				UIWindowScene scene = uiWindow.getWindowScene();
				UIInterfaceOrientation previousOrientation = orientation;
				orientation = scene.getInterfaceOrientation();
				getView().setContentScaleFactor(scene.getScreen().getNativeScale());
				super.viewDidLayoutSubviews();
				IOSScreenBounds current = graphics.screenBounds;
				if (current.width != previous.width || current.height != previous.height) {
					return;
				}
				graphics.updateSafeInsets();
				if (config.hdpiMode == HdpiMode.Pixels && (current.backBufferWidth != previous.backBufferWidth
						|| current.backBufferHeight != previous.backBufferHeight)) {
					graphics.makeCurrent();
					graphics.gl20.glViewport(0, 0, current.backBufferWidth, current.backBufferHeight);
					listener.resize(current.backBufferWidth, current.backBufferHeight);
				} else if (orientation != previousOrientation || left != graphics.getSafeInsetLeft()
						|| top != graphics.getSafeInsetTop() || right != graphics.getSafeInsetRight()
						|| bottom != graphics.getSafeInsetBottom()) {
					displayInsetsChanged();
				}
			}
		};
	}
}
