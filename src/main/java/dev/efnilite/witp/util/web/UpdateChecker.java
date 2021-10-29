package dev.efnilite.witp.util.web;

import dev.efnilite.witp.WITP;
import dev.efnilite.witp.util.Verbose;
import dev.efnilite.witp.util.task.Tasks;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker {

    public void check() {
        Tasks.asyncTask(() -> {
            String latest;
            try {
                latest = getLatestVersion();
            } catch (IOException ex) {
                ex.printStackTrace();
                Verbose.error("Error while trying to fetch latest version!");
                return;
            }
            if (!WITP.getInstance().getDescription().getVersion().equals(latest)) {
                Verbose.info("A new version of WITP is available to download!");
                Verbose.info("Newest version: " + latest);
                WITP.OUTDATED = true;
            } else {
                Verbose.info("WITP is currently up-to-date!");
            }
        });
    }

    private String getLatestVersion() throws IOException {
        InputStream stream;
        Verbose.info("Checking for updates...");
        try {
            stream = new URL("https://raw.githubusercontent.com/Efnilite/Walk-in-the-Park/master/src/main/resources/plugin.yml").openStream();
        } catch (IOException e) {
            Verbose.info("Unable to check for updates!");
            return "";
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            return reader.lines()
                    .filter(s -> s.contains("version: ") && !s.contains("api"))
                    .collect(Collectors.toList())
                    .get(0)
                    .replace("version: ", "");
        }
    }
}