import java.io.File;
import java.io.FilePermission;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;

/**
 * Created by yang on 16-11-28.
 */
public class Invoke {
    //AccessControlContext.doPrivilrged意思是不做权限检查
 /*   public void doCheck(){
        AccessController.doPrivileged(new PrivilegedAction() {
            @Override
            public Object run() {
                check();
                return null;
            }
        });
    }*/
    public void doCheck(){
        check();
    }
    private void check(){
        Permission perm = new FilePermission("./largeFile.txt","read");
        AccessController.checkPermission(perm);
        System.out.println("TestService has permission");
    }

}
