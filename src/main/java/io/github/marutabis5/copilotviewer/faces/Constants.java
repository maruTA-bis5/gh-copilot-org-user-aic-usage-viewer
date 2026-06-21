package io.github.marutabis5.copilotviewer.faces;

import jakarta.enterprise.inject.Model;

import java.io.IOException;
import java.util.Properties;

@Model
public class Constants {

    private static final String GIT_COMMIT_ID_SHORT;

    static {
        try {
            var props = new Properties();
            props.load(Constants.class.getClassLoader().getResourceAsStream("git.properties"));
            GIT_COMMIT_ID_SHORT = String.valueOf(props.get("git.commit.id.describe-short"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String getGitCommitIdShort() {
        return GIT_COMMIT_ID_SHORT;
    }
}


