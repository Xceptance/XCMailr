/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package conf;

import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;
import ninja.utils.NinjaProperties;

import com.google.inject.Inject;

import controllers.AdminHandler;
import controllers.Application;
import controllers.BoxHandler;
import controllers.UserHandler;


public class Routes implements ApplicationRoutes {
    
    private NinjaProperties ninjaProperties;

    @Inject
    public Routes(NinjaProperties ninjaProperties) {
        this.ninjaProperties = ninjaProperties;


    }

    /**
     * Using a (almost) nice DSL we can configure the router.
     * 
     * The second argument NinjaModuleDemoRouter contains all routes of a
     * submodule. By simply injecting it we activate the routes.
     * 
     * @param router
     *            The default router of this application
     */
    @Override
    public void init(Router router) {

    	
    	
        // /////////////////////////////////////////////////////////////////////
        // CCMailr-Functions
        // /////////////////////////////////////////////////////////////////////
        router.GET().route("/").with(Application.class, "index");
        
        router.GET().route("/register").with(Application.class, "registerForm");
        router.POST().route("/register").with(Application.class, "postRegisterForm");        
        
        router.GET().route("/login").with(Application.class, "loginForm");
        router.POST().route("/login").with(Application.class, "loginProc");
        
        router.GET().route("/pwresend").with(Application.class, "forgotPwForm");
        router.POST().route("/pwresend").with(Application.class,"pwResend");
        
        router.GET().route("/logout").with(Application.class,"logout");
        
        //router.GET().route("/hidden/users").with(Application.class, "userlist");
   
        
        router.GET().route("/user/edit").with(UserHandler.class, "editUserForm");
        router.POST().route("/user/edit").with(UserHandler.class,"editUser");
        
        router.GET().route("/mail").with(BoxHandler.class, "showBoxes");
        
        
        router.GET().route("/mail/add").with(BoxHandler.class, "showAddBox");
        router.POST().route("/mail/add").with(BoxHandler.class, "addBox");
        router.POST().route("/mail/expire/{id}").with(BoxHandler.class,"expireBox");
        router.POST().route("/mail/delete/{id}").with(BoxHandler.class, "deleteBox");
        router.GET().route("/mail/edit/{id}").with(BoxHandler.class, "showEditBox");
        router.POST().route("/mail/edit/{id}").with(BoxHandler.class, "editBox");

        router.GET().route("/admin/promote/{id}").with(AdminHandler.class, "promoteUser");
        router.GET().route("/admin/delete/{id}").with(AdminHandler.class, "deleteUser");
        router.GET().route("/admin").with(AdminHandler.class,"showUsers");
        
        
        
        
        
        
        
//        
//        //this is a route that should only be accessible when NOT in production
//        // this is tested in RoutesTest
//        if (!ninjaProperties.isProd()) {
//            router.GET().route("/_test/testPage").with(ApplicationController.class, "testPage");
//        }
//
        router.GET().route("/assets/.*").with(AssetsController.class, "serve");
        
        

        // /////////////////////////////////////////////////////////////////////
        // some default functions
        // /////////////////////////////////////////////////////////////////////
        // simply render a page:
        //router.GET().route("/").with(ApplicationController.class, "index");
//        router.GET().route("/examples").with(ApplicationController.class, "examples");
//
//        // render a page with variable route parts:
//        router.GET().route("/user/{id}/{email}/userDashboard").with(ApplicationController.class, "userDashboard");
//
//        router.GET().route("/validation").with(ApplicationController.class, "validation");
//
//        // redirect back to /
//        router.GET().route("/redirect").with(ApplicationController.class, "redirect");
//
//        router.GET().route("/session").with(ApplicationController.class, "session");
//
//        router.GET().route("/htmlEscaping").with(ApplicationController.class, "htmlEscaping");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Json support
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/person").with(PersonController.class, "getPerson");
//        router.POST().route("/person").with(PersonController.class, "postPerson");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Form parsing support
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/contactForm").with(ApplicationController.class, "contactForm");
//        router.POST().route("/contactForm").with(ApplicationController.class, "postContactForm");
//
//        
//        // /////////////////////////////////////////////////////////////////////
//        // Lifecycle support
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/udpcount").with(UdpPingController.class, "getCount");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Route filtering example:
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/filter").with(FilterController.class, "filter");
//        router.GET().route("/teapot").with(FilterController.class, "teapot");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Route filtering example:
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/injection").with(InjectionExampleController.class, "injection");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Async example:
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/async").with(AsyncController.class, "asyncEcho");
//
//        // /////////////////////////////////////////////////////////////////////
//        // I18n:
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/i18n").with(I18nController.class, "index");
//        router.GET().route("/i18n/{language}").with(I18nController.class, "indexWithLanguage");
//
//        // /////////////////////////////////////////////////////////////////////
//        // Upload showcase
//        // /////////////////////////////////////////////////////////////////////
//        router.GET().route("/upload").with(UploadController.class, "upload");
//        router.POST().route("/uploadFinish").with(UploadController.class, "uploadFinish");
//        
        
    	
        
        
        
        
    }

}
