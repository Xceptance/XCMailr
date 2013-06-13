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
import controllers.AdminHandler;
import controllers.Application;
import controllers.BoxHandler;
import controllers.UserHandler;

public class Routes implements ApplicationRoutes
{

    @Override
    public void init(Router router)
    {
        
        router.GET().route("/").with(Application.class, "index");
        router.GET().route("/register").with(Application.class, "registerForm");
        router.POST().route("/register").with(Application.class, "registrationProcess");

        router.GET().route("/login").with(Application.class, "loginForm");
        router.POST().route("/login").with(Application.class, "logInProcess");

        router.GET().route("/pwresend").with(Application.class, "forgotPasswordForm");
        router.POST().route("/pwresend").with(Application.class, "forgotPasswordProcess");
        
        router.GET().route("/lostpw/{id}/{token}").with(Application.class, "lostPasswordForm");
        router.POST().route("/lostpw/{id}/{token}").with(Application.class, "lostPasswordProcess");
        
        router.GET().route("/verify/{id}/{token}").with(Application.class, "verifyActivation");

        router.GET().route("/logout").with(Application.class, "logoutProcess");
        
        //Routes for UserHandling (after registration)
        router.GET().route("/user/edit").with(UserHandler.class, "editUserForm");
        router.POST().route("/user/edit").with(UserHandler.class, "editUserProcess");

        //Routes for the Mail-Handling 
        router.GET().route("/mail").with(BoxHandler.class, "showBoxOverview");
        router.POST().route("/mail").with(BoxHandler.class, "showBoxOverview");
        
        router.GET().route("/mail/add").with(BoxHandler.class, "addBoxForm");
        router.POST().route("/mail/add").with(BoxHandler.class, "addBoxProcess");
        
        router.GET().route("/mail/edit/{id}").with(BoxHandler.class, "editBoxForm");
        router.POST().route("/mail/edit/{id}").with(BoxHandler.class, "editBoxProcess");
        
        router.POST().route("/mail/expire/{id}").with(BoxHandler.class, "expireBoxProcess");
        router.POST().route("/mail/delete/{id}").with(BoxHandler.class, "deleteBoxProcess");
        router.POST().route("/mail/reset/{id}").with(BoxHandler.class, "resetBoxCounterProcess");
        

        
        //Routes in the admin-section
        router.POST().route("/admin/promote/{id}").with(AdminHandler.class, "promoteUserProcess");
        router.POST().route("/admin/activate/{id}").with(AdminHandler.class, "activateUserProcess");
        router.POST().route("/admin/delete/{id}").with(AdminHandler.class, "deleteUserProcess");
        router.GET().route("/admin").with(AdminHandler.class, "showAdmin");
        router.GET().route("/admin/users").with(AdminHandler.class, "showUsers");
        router.POST().route("/admin/users").with(AdminHandler.class, "showUsers");
        router.GET().route("/admin/summedtx").with(AdminHandler.class, "showSumTx");
        router.GET().route("/admin/mtxs").with(AdminHandler.class, "pagedMTX");
        router.POST().route("/admin/mtxs").with(AdminHandler.class, "pagedMTX");
        router.GET().route("/admin/mtxs/delete/{time}").with(AdminHandler.class, "deleteMTXProcess");
        
        //Assets-Handling
        router.GET().route("/assets/.*").with(AssetsController.class, "serve");

    }

}
