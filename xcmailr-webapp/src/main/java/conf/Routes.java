/*
 * Copyright (c) 2013-2023 Xceptance Software Technologies GmbH
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

import controllers.AdminHandler;
import controllers.Application;
import controllers.BoxHandler;
import controllers.UserHandler;
import controllers.restapi.MailApiController;
import controllers.restapi.MailboxApiController;
import ninja.AssetsController;
import ninja.Router;
import ninja.application.ApplicationRoutes;

public class Routes implements ApplicationRoutes
{

    @Override
    public void init(Router router)
    {

        /*
         * Routes for the General Application-Handling (Controller: Application)
         * 
         * Contains all actions which do not require a user who's logged in
         */
        router.GET().route("/").with(Application::index);

        router.GET().route("/login").with(Application::loginForm);
        router.POST().route("/login").with(Application::logInProcess);

        router.GET().route("/logout").with(Application::logoutProcess);

        router.GET().route("/lostpw/{id}/{token}").with(Application::resetPasswordForm);
        router.POST().route("/lostpw/{id}/{token}").with(Application::resetPasswordProcess);

        router.GET().route("/pwresend").with(Application::forgotPasswordForm);
        router.POST().route("/pwresend").with(Application::forgotPasswordProcess);

        router.GET().route("/register").with(Application::registerForm);
        router.POST().route("/register").with(Application::registrationProcess);

        router.GET().route("/verify/{id}/{token}").with(Application::verifyActivation);

        router.POST().route("/getMessage").with(Application::getStatusMessage);

        /*
         * Routes for UserHandling (after login) (Controller: UserHandler)
         */
        router.GET().route("/user/edit").with(UserHandler::editUserForm);
        router.POST().route("/user/edit").with(UserHandler::editUserProcess);

        router.POST().route("/user/delete").with(UserHandler::deleteUserProcess);

        router.GET().route("/user/newApiToken").with(UserHandler::createNewApiToken);
        router.GET().route("/user/revokeApiToken").with(UserHandler::revokeApiToken);

        /*
         * Routes for the Mail-Handling (Controller: BoxHandler)
         */
        router.GET().route("/mail").with(BoxHandler::showAngularBoxOverview);
        router.POST().route("/mail").with(BoxHandler::showAngularBoxOverview);
        router.GET().route("/mail/getmails").with(BoxHandler::jsonBox);
        router.GET().route("/mail/editBoxDialog.html").with(BoxHandler::editBoxDialog);
        router.GET().route("/mail/deleteBoxDialog.html").with(BoxHandler::deleteBoxDialog);
        router.GET().route("/mail/newDateDialog.html").with(BoxHandler::newDateDialog);

        router.GET().route("/mail/addAddressData").with(BoxHandler::addBoxJsonData);
        router.POST().route("/mail/addAddress").with(BoxHandler::addBoxJsonProcess);

        router.POST().route("/mail/bulkDelete").with(BoxHandler::bulkDeleteBoxes);
        router.POST().route("/mail/bulkReset").with(BoxHandler::bulkResetBoxes);
        router.POST().route("/mail/bulkDisable").with(BoxHandler::bulkDisableBoxes);
        router.POST().route("/mail/bulkEnablePossible").with(BoxHandler::bulkEnablePossibleBoxes);
        router.POST().route("/mail/bulkNewDate").with(BoxHandler::bulkNewDate);

        router.POST().route("/mail/delete/{id}").with(BoxHandler::deleteBoxByJson);
        //
        router.POST().route("/mail/edit/{id}").with(BoxHandler::editBoxJson);
        router.POST().route("/mail/expire/{id}").with(BoxHandler::expireBoxJson);

        router.GET().route("/mail/mymaillist.txt").with(BoxHandler::showMailsAsTextList);
        router.GET().route("/mail/myactivemaillist.txt").with(BoxHandler::showActiveMailsAsTextList);
        router.GET().route("/mail/myselectedmaillist.txt").with(BoxHandler::showSelectedMailsAsTextList);
        router.POST().route("/mail/myselectedmaillist.txt").with(BoxHandler::showSelectedMailsAsTextList);

        router.POST().route("/mail/reset/{id}").with(BoxHandler::resetBoxCounterProcessXhr);

        router.GET().route("/mail/domainlist").with(BoxHandler::jsonDomainList);

        router.GET().route("/create/temporaryMail/{token}/{mailAddress}/{validTime}")
              .with(BoxHandler::createTemporaryMailAddress);

        router.GET().route("/mailbox/{mailAddress}/{token}").with(BoxHandler::queryMailbox);
        router.GET().route("/mailbox").with(BoxHandler::queryMailbox);
        router.GET().route("/mails").with(BoxHandler::queryAllMailboxes);
        router.GET().route("/download/{downloadToken}/{filename}").with(BoxHandler::downloadMailAttachment);

        /*
         * Routes in the admin-section (Controller: AdminHandler)
         */
        router.GET().route("/admin").with(AdminHandler::showAdmin);

        router.POST().route("/admin/activate/{id}").with(AdminHandler::activateUserProcess);

        router.POST().route("/admin/delete/{id}").with(AdminHandler::deleteUserProcess);

        router.GET().route("/admin/mtxs").with(AdminHandler::pagedMTX);
        router.POST().route("/admin/mtxs").with(AdminHandler::pagedMTX);
        router.GET().route("/admin/mtxs/delete/{time}").with(AdminHandler::deleteMTXProcess);

        router.POST().route("/admin/promote/{id}").with(AdminHandler::promoteUserProcess);

        router.GET().route("/admin/summedtx").with(AdminHandler::showSummedTransactions);

        router.GET().route("/admin/users").with(AdminHandler::showUsers);
        router.POST().route("/admin/users").with(AdminHandler::showUsers);

        router.GET().route("/admin/usersearch").with(AdminHandler::jsonUserSearch);

        router.GET().route("/admin/whitelist").with(AdminHandler::showDomainWhitelist);
        router.POST().route("/admin/whitelist/remove").with(AdminHandler::callRemoveDomain);
        router.GET().route("/admin/whitelist/remove").with(AdminHandler::handleRemoveDomain);
        router.POST().route("/admin/whitelist/add").with(AdminHandler::addDomain);

        router.GET().route("/admin/emailStatistics").with(AdminHandler::showEmailStatistics);
        router.GET().route("/admin/emailSenderPage").with(AdminHandler::getEmailSenderTablePage);

        /*
         * REST API
         */

        // mailboxes
        router.GET().route("/api/v1/mailboxes").with(MailboxApiController::listMailboxes);
        router.POST().route("/api/v1/mailboxes").with(MailboxApiController::createMailbox);
        router.GET().route("/api/v1/mailboxes/{mailboxAddress}").with(MailboxApiController::getMailbox);
        router.PUT().route("/api/v1/mailboxes/{mailboxAddress}").with(MailboxApiController::updateMailbox);
        router.DELETE().route("/api/v1/mailboxes/{mailboxAddress}").with(MailboxApiController::deleteMailbox);

        // mails
        router.GET().route("/api/v1/mails").with(MailApiController::listMails);
        router.GET().route("/api/v1/mails/{mailId}").with(MailApiController::getMail);
        router.DELETE().route("/api/v1/mails/{mailId}").with(MailApiController::deleteMail);
        router.GET().route("/api/v1/mails/{mailId}/attachments/{attachmentName}")
              .with(MailApiController::getMailAttachment);

        /*
         * Assets-Handling (Ninja's AssetsController)
         */
        router.GET().route("/assets/webjars/{fileName: .*}").with(AssetsController::serveWebJars);
        router.GET().route("/assets/{fileName: .*}").with(AssetsController::serveStatic);
    }
}
