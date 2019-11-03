package com.github.skoved.uniqueemails;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.routines.EmailValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/email")
public class EmailController {
    private final Logger log;
    private final EmailValidator emailValidator;

    public EmailController() {
        log = LoggerFactory.getLogger(EmailController.class);
        emailValidator = EmailValidator.getInstance(false, false);
    }

    @PostMapping("/validate")
    int validateEmails(@Valid @RequestBody List<String> emails) {
        if (emails == null) {
            throw new IllegalArgumentException("Please provide a list of valid emails.");
        }

        List<String> invalidEmails = new ArrayList<>();
        for (int i = 0; i < emails.size(); i++) {
            String curr = emails.get(i);
            if (curr.equals("")) {
                invalidEmails.add(curr);
                continue;
            }
            String email = removeComments(curr);
            email = unwrap(email);
            //EmailValidator in Commons-Validator in v1.6 has a know issue of validating emails with an opening
            if (!emailValidator.isValid(email) || email.charAt(0) == ' ') {
                invalidEmails.add(curr);
            } else {
                emails.remove(i);
                emails.add(i, email);
            }
        }

        if (!invalidEmails.isEmpty()) {
            throw new IllegalArgumentException("The email(s) " + invalidEmails + " is/are invalid.");
        }

        Set<String> uniqueEmails = new HashSet<>();
        for (String email : emails) {
            email = formatEmail(email);
            uniqueEmails.add(email);
        }
        return uniqueEmails.size();
    }

    String formatEmail(String email) {
        String ret = email.replace(".", "");
        ret = ret.replaceAll("\\([\\s\\S]*?\\)", "");
        ret = ret.replaceAll("\\+\\S*@", "@");
        if (ret.contains("!")) {
            boolean inQuotes = false;
            String iterVal = ret.split("@")[0];
            String[] split = new String[2];
            for (int i = 0; i< iterVal.length(); i++) {
                char curr = iterVal.charAt(i);
                if (inQuotes && curr != '\"') {
                    continue;
                } else if (curr == '!') {
                    split[0] = iterVal.substring(0, i);
                    split[1] = iterVal.substring(i + 1);
                } else if (curr == '\"') {
                    inQuotes = !inQuotes;
                }
            }
            if (split[0] != null && split[1] != null) {
                return split[1] + "@" + split[0];
            }

            return ret;
        }

        if (ret.contains("%")) {
            String[] split = ret.split("@");
            split = split[0].split("%");
            return split[0] + "@" + split[1];
        }

        return ret;
    }

    String removeComments(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        boolean inComment = false;
        boolean inQuotes = false;
        String[] parts = new String[2];
        for (int i = 0; i < email.length(); i++) {
            char curr = email.charAt(i);
            if (inQuotes && curr != '\"') {
                continue;
            } else if (inComment && curr != ')') {
                continue;
            }

            switch (curr) {
                case '(':
                    inComment = true;
                    break;
                case ')':
                    inComment = false;
                    break;
                case '\"':
                    inQuotes = !inQuotes;
                    break;
                case '@':
                    parts[0] = email.substring(0, i);
                    parts[1] = email.substring(i + 1);
            }
        }

        if (inComment || inQuotes || (parts[0] == null || parts[1] == null)) {
            return null;
        }

        for (int x = 0; x < parts.length; x++) {
            int parenDepth = 0;
            int end = -1;
            inQuotes = false;
            for (int i = 0; i < parts[x].length() && parts[x].charAt(0) == '('; i++) {
                if (inQuotes && parts[x].charAt(i) != '\"') {
                    continue;
                }
                switch (parts[x].charAt(i)) {
                    case '(':
                        parenDepth++;
                        break;
                    case ')':
                        parenDepth--;
                        if (parenDepth < 0) {
                            return null;
                        }
                        if (parenDepth == 0) {
                            end = i + 1;
                            parts[x] = parts[x].substring(end);
                            i = 0;
                            end = -1;
                        }
                        break;
                    case '\"':
                        inQuotes = !inQuotes;
                        break;
                    case '\\':
                        i++;
                        break;
                    default:
                        break;
                }
            }
            if (parenDepth != 0 || inQuotes) {
                return null;
            }

            parenDepth = 0;
            end = -1;
            inQuotes = false;
            for (int i = parts[x].length() - 1; i > 0 && parts[x].charAt(parts[x].length() - 1) == ')'; i--) {
                if (inQuotes && parts[x].charAt(i) != '\"') {
                    continue;
                }
                switch (parts[x].charAt(i)) {
                    case ')':
                        parenDepth++;
                        break;
                    case '(':
                        if (parts[x].charAt(i - 1) != '\\') {
                            parenDepth--;
                        } else {
                            i--;
                            continue;
                        }
                        if (parenDepth < 0) {
                            return null;
                        }
                        if (parenDepth == 0) {
                            end = i;
                            parts[x] = parts[x].substring(0, end);
                            i = parts[x].length() - 1;
                            end = -1;
                        }
                        break;
                    case '\"':
                        inQuotes = !inQuotes;
                        break;
                    default:
                        continue;
                }
            }
            if (parenDepth != 0 || inQuotes) {
                return null;
            }
        }

        return parts[0] + "@" + parts[1];
    }

    String unwrap(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int begin = -1;
        int end = -1;
        boolean inQuotes = false;
        for (int i = 0; i < email.length(); i++) {
            char curr = email.charAt(i);
            if (inQuotes && curr != '\"') {
                continue;
            }
            switch(curr) {
                case '\"':
                    inQuotes = !inQuotes;
                    break;
                case '<':
                    begin = i+1;
                    break;
                case '>':
                    end = i;
                    break;
                default:
                    break;
            }
        }

        if (begin != -1 && end != -1) {
            return email.substring(begin, end);
        }

        return email;
    }
}
