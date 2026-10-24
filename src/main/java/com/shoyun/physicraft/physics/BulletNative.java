package com.shoyun.physicraft.physics;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import com.jme3.system.JmeSystem;
import com.jme3.system.Platform;

/**
 * Loads the Libbulletjme native library shipped inside this mod.
 * Desktop artifacts keep the library at {@code native/<os>/<arch>/...}; we copy that resource out and {@code System.load} it.
 */
public final class BulletNative {
    private static final Object LOCK = new Object();
    private static volatile boolean loaded;
    private static volatile String failureReason = "not loaded";
    private static volatile String version = "unknown";
    private static volatile String platformName = "unknown";

    private BulletNative() {
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static String failureReason() {
        return failureReason;
    }

    public static String version() {
        return version;
    }

    public static String platform() {
        return platformName;
    }

    public static boolean ensureLoaded() {
        if (loaded) {
            return true;
        }
        synchronized (LOCK) {
            if (loaded) {
                return true;
            }
            try {
                version = readVersion();
                Platform platform = JmeSystem.getPlatform();
                platformName = platform.name();
                NativeLibrary library = libraryFor(platform);
                if (library == null) {
                    failureReason = "unsupported platform " + platformName;
                    return false;
                }
                Path extracted = extract(library, version);
                System.load(extracted.toAbsolutePath().toString());
                loaded = true;
                failureReason = "";
                return true;
            } catch (Throwable throwable) {
                failureReason = throwable.toString();
                return false;
            }
        }
    }

    private static String readVersion() throws IOException {
        try (InputStream in = BulletNative.class.getClassLoader().getResourceAsStream("bullet-native.properties")) {
            if (in == null) {
                return "unknown";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "unknown");
        }
    }

    private static Path extract(NativeLibrary library, String nativeVersion) throws IOException {
        String resource = "assets/physicraft/natives/" + library.resourcePath();
        try (InputStream in = BulletNative.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("missing native resource " + resource);
            }
            Path directory = Path.of(System.getProperty("java.io.tmpdir"), "physicraft", "bullet-" + nativeVersion);
            Files.createDirectories(directory);
            Path target = directory.resolve(library.fileName());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            return target;
        }
    }

    private static NativeLibrary libraryFor(Platform platform) {
        return switch (platform) {
            case Linux64 -> new NativeLibrary("linux/x86_64/libbulletjme.so", "libbulletjme.so");
            case Linux_ARM64 -> new NativeLibrary("linux/arm64/libbulletjme.so", "libbulletjme.so");
            case Windows64 -> new NativeLibrary("windows/x86_64/bulletjme.dll", "bulletjme.dll");
            case Windows_ARM64 -> new NativeLibrary("windows/arm64/bulletjme.dll", "bulletjme.dll");
            case MacOSX64 -> new NativeLibrary("osx/x86_64/libbulletjme.dylib", "libbulletjme.dylib");
            case MacOSX_ARM64 -> new NativeLibrary("osx/arm64/libbulletjme.dylib", "libbulletjme.dylib");
            default -> null;
        };
    }

    private record NativeLibrary(String resourcePath, String fileName) {
    }
}
