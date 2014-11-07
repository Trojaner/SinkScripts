/*
 * Copyright (c) 2014 http://adventuria.eu, http://static-interface.de and contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.static_interface.sinkscripts.util;

import de.static_interface.sinklibrary.SinkLibrary;
import de.static_interface.sinklibrary.util.StringUtil;
import de.static_interface.sinkscripts.SinkScripts;
import de.static_interface.sinkscripts.scriptengine.ScriptHandler;
import de.static_interface.sinkscripts.scriptengine.scriptlanguage.ScriptLanguage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;

import javax.annotation.Nullable;
import javax.script.ScriptException;

public class Util {

    public static String getFileExtension(File file) {
        String tmp = file.getName();
        int i = tmp.lastIndexOf('.');

        if (i > 0 && i < tmp.length() - 1) {
            return tmp.substring(i + 1).toLowerCase();
        }

        return null;
    }

    public static String getNewLine() {
        return System.getProperty("line.separator");
    }

    public static void reportException(CommandSender sender, Throwable e) {
        if (e == null) {
            return;
        }

        SinkScripts.getInstance().getLogger().log(Level.WARNING, "[SinkScripts] Exception caused by " + sender.getName() + ": ");
        e.printStackTrace();

        //bad :(
        if(e instanceof RuntimeException && e.getCause() instanceof ScriptException && e.getMessage().contains(e.getCause().getMessage())) {
            e = e.getCause();
        }

        while (e instanceof ScriptException) {
            if(e.getCause() == null) break;
            e = e.getCause();
        }

        sender.sendMessage(ChatColor.DARK_RED + "Unexpected " + ((e instanceof Exception) ? "exception: " : "error (see console for more details): "));
        String[] lines = null;
        if (e.getMessage() != null) {
            lines = e.getMessage().split(Util.getNewLine());
        }
        String msg = null;
        if (lines != null && lines.length > 0) {
            msg = lines[0] + (lines.length > 1 ? Util.getNewLine() + lines[1] : "");
        }
        sender.sendMessage(ChatColor.RED + e.getClass().getCanonicalName() + (msg == null ? "" : ": " + msg));

        Throwable cause = e.getCause();
        if(cause == null) {
            return;
        }

        //Report all "Caused By" exceptions but don't show stacktraces
        while (cause != null) {
            lines = null;

            if (cause.getMessage() != null) {
                lines = cause.getMessage().split(Util.getNewLine());
            }
            msg = null;
            if (lines != null && lines.length > 0) {
                msg = lines[0] + (lines.length > 1 ? Util.getNewLine() + lines[1] : "");
            }
            sender.sendMessage(ChatColor.RED + "Caused by: " + cause.getClass().getCanonicalName() + (msg == null ? "" : ": " + msg));

            cause = cause.getCause();
        }
    }

    public static String loadFile(File scriptFile) throws IOException {
        String nl = Util.getNewLine();
        if (!scriptFile.exists()) {
            throw new FileNotFoundException("Couldn't find file: " + scriptFile);
        }
        BufferedReader br = new BufferedReader(new FileReader(scriptFile));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();

        while (line != null) {
            sb.append(line);
            sb.append(nl);
            line = br.readLine();
        }
        return sb.toString();
    }

    public static String loadFile(String scriptName, @Nullable ScriptLanguage language) throws IOException {
        String tmp[] = scriptName.split("\\.");
        if(tmp.length < 1 && language == null) {
            throw new IllegalStateException("Couldn't find extension:");
        }

        String extension = tmp[tmp.length -1];
        ScriptLanguage tmpLanguage = ScriptHandler.getScriptLanguageByExtension(extension); // get language by extension

        if(tmpLanguage == null && language == null) {
            throw new IllegalArgumentException("Couldn't find ScriptLanguage instance for input: " + scriptName);
        }

        if(tmpLanguage != null) {
            language = tmpLanguage;
        }

        scriptName = StringUtil.formatArrayToString(tmp, ".", 0, tmp.length - 1); // remove extension

        File scriptFile = new File(language.SCRIPTLANGUAGE_DIRECTORY, scriptName + "." + language.getFileExtension());
        if (!scriptFile.exists()) {
            SinkLibrary.getInstance().getCustomLogger().debug("Couldn't find file: " + scriptFile.getAbsolutePath());
            scriptFile = Util.searchRecursively(scriptName, language.SCRIPTLANGUAGE_DIRECTORY, language);
        }
        return loadFile(scriptFile);
    }


    public static File searchRecursively(String scriptName, File directory, ScriptLanguage language) throws IOException {
        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                return searchRecursively(scriptName, file, language);
            } else {
                if (file.getName().equals(scriptName + "." + language.getFileExtension())) {
                    return file;
                }
            }
        }
        return null;
    }

}
