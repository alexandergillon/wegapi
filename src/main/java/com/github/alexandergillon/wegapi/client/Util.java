package com.github.alexandergillon.wegapi.client;

import com.github.alexandergillon.wegapi.game.DaemonInterface;

import java.io.File;
import java.nio.file.Path;

public final class Util {
    private Util() {
        throw new AssertionError("Utility class, not meant to be instantiated");
    }

    /**
     * Checks whether a path exists, and optionally whether it is a directory. If either check fails, prints an
     * error message and aborts.
     *
     * @param path path to check whether it exists
     * @param checkIsDir whether to check if the path is also a directory
     */
    static void checkExists(Path path, boolean checkIsDir) {
        File file = new File(path.toString());
        if (!file.exists()) {
            System.out.println(path + " does not exist, aborting.");
            System.exit(1);
        }

        if (checkIsDir && !file.isDirectory()) {
            System.out.println(path + " is not a directory, aborting.");
            System.exit(1);
        }
    }

    // these are here because they would have to declare throwing RemoteException if they were in DaemonInterface,
    // despite this never being possible
    public static String buildDaemonRMIPath(int daemonNumber) {
        return "//" + DaemonInterface.DEFAULT_IP + ":" + DaemonInterface.RMI_REGISTRY_PORT + "/" + DaemonInterface.DEFAULT_DAEMON_PATH + daemonNumber;
    }

    public static String buildDaemonRMIPath(String ip, int port, int daemonNumber) {
        return "//" + ip + ":" + port + "/" + DaemonInterface.DEFAULT_DAEMON_PATH + daemonNumber;
    }
}
