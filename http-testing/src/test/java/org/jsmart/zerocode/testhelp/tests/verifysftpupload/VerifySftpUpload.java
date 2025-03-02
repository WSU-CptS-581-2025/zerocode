package org.jsmart.zerocode.testhelp.tests.verifysftpupload;

import org.jsmart.zerocode.core.domain.Scenario;
import org.jsmart.zerocode.core.domain.TargetEnv;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jsmart.zerocode.core.runner.ZeroCodeUnitRunner;

import org.jsmart.zerocode.utilities.SFTPUploader;

@TargetEnv("github_host.properties")
@RunWith(ZeroCodeUnitRunner.class)
public class VerifySftpUpload {

    @Test
    @Scenario("verify_sftp_upload/verify_log_is_uploaded_to_sftp.json")
    public void verifyUpload() throws Exception {
    }

    @AfterClass
    public static void afterTests() {
        System.out.println("Uploading logs to SFTP...");
        boolean wasSuccessful = SFTPUploader.uploadLogFile(); 

        if(wasSuccessful)
        {
            System.out.println("TEST PASSED! File successfully uploaded to sftp.");
        }
        else
        {
            System.out.println("TEST FAILED: unable to upload log to sftp.");
        }
    }
}
