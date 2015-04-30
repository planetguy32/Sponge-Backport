/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.launch;

import org.spongepowered.launch.handlers.GoHandler;
import org.spongepowered.launch.handlers.HelpHandler;
import org.spongepowered.launch.handlers.ResolveHandler;
import org.spongepowered.launch.handlers.RunHandler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Bootstrap class to provide some help for end users and manage headless
 * self-install deployment via Ivy
 */
public class Main {

    private static final File JAR = Main.getJar();
    public static final String JAR_NAME = Main.getJarName();
    
    private static final URL IVY_SETTINGS_RESOURCE = Main.class.getResource("ivysettings.xml");
    private static final URL IVY_RESOURCE = Main.class.getResource("ivy.xml");
    
    private static Attributes manifestAttributes = null; 
    
    private static final Map<String, LaunchHandler> handlers = new LinkedHashMap<String, LaunchHandler>();
    
    public static void main(String[] argv) {
        
        System.out.printf("\n%s v%s for Minecraft Forge %s\n   implementing %s version %s\n",
                Main.getManifestAttribute("Implementation-Name", "Sponge"),
                Main.getManifestAttribute("Implementation-Version", "DEV"),
                Main.getManifestAttribute("TargetForgeBuild", ""),
                Main.getManifestAttribute("Specification-Name", "SpongeAPI"),
                Main.getManifestAttribute("Specification-Version", "DEV")
        );
        
        List<String> args = new ArrayList<String>(Arrays.asList(argv));
        Main.initDefaultHandlers();
        try {
            Main.dispatch(args);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private static void dispatch(List<String> args) {
        if (args.size() > 0) {
            String command = Main.getCommand(args);
            LaunchHandler handler = Main.handlers.get(command);
            if (handler == null) {
                System.out.printf("\nAn invalid option was specified!\n");
                handler = Main.handlers.get("help");
            }
            handler.acceptArguments(args);
            handler.run();
        } else {
            Main.printUsage();
        }
    }

    private static String getCommand(List<String> args) {
        String command = args.remove(0).toLowerCase();
        return command.startsWith("--") ? command.substring(2) : command;
    }

    private static void printUsage() {
        Scanner help = new Scanner(Main.class.getResourceAsStream("help.txt")).useDelimiter("\\Z");
        System.out.print(help.next().replace("${jarfile}", Main.JAR_NAME));
    }

    private static void initDefaultHandlers() {
        Main.addHandler(new HelpHandler());
        Main.addHandler(new ResolveHandler(Main.IVY_SETTINGS_RESOURCE, Main.IVY_RESOURCE));
        Main.addHandler(new RunHandler(Main.IVY_SETTINGS_RESOURCE, Main.IVY_RESOURCE));
        Main.addHandler(new GoHandler(Main.IVY_SETTINGS_RESOURCE, Main.IVY_RESOURCE));
    }

    public static void addHandler(LaunchHandler handler) {
        Main.handlers.put(handler.getName().toLowerCase(), handler);
    }
    
    public static Collection<LaunchHandler> getHandlers() {
        return Collections.unmodifiableCollection(Main.handlers.values());
    }

    private static File getJar() {
        try {
            File jar = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (jar.isFile()) {
                return jar;
            }
        } catch (Exception ex) {
            // derp
        }
        
        return null;
    }
    
    private static String getJarName() {
        return Main.JAR != null ? Main.JAR.getName() : "sponge.jar";
    }

    public static String getManifestAttribute(String key, String defaultValue) {
        if (Main.JAR == null) {
            return defaultValue;
        }
        
        Attributes manifestAttributes = Main.getManifestAttributes();
        return manifestAttributes != null ? manifestAttributes.getValue(key) : defaultValue;
    }

    private static Attributes getManifestAttributes() {
        if (Main.manifestAttributes == null) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(Main.JAR);
                Main.manifestAttributes = jarFile.getManifest().getMainAttributes();
                
            } catch (IOException ex) {
                // be quiet checkstyle
            } finally {
                if (jarFile != null) {
                    try {
                        jarFile.close();
                    } catch (IOException ex) {
                        // this could be an issue later on :(
                    }
                }
            }
        }
        
        return Main.manifestAttributes;
    }

}
