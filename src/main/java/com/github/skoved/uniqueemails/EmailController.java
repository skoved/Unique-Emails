package com.github.skoved.uniqueemails;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creates an endpoint at http://[your domain]:8080/email/validate. The endpoint calculates
 * the number of unique emails there are in a list of strings. It does so by using the
 * EmailValidator found in Apache commons-validator in tandem with custom string processing
 * algorithms.
 */
@RestController
@RequestMapping("/email")
public class EmailController {
    private final EmailValidator emailValidator;

    /**
     * Creates a new EmailController and requests an instance of the EmailValidator
     */
    public EmailController() {
        emailValidator = EmailValidator.getInstance(false, false);
    }

    /**
     * Listens for HTTP POST requests to [your domain]:8080/email/validate. Processes each string
     * and determines whether or not the string is a valid email address. Then determines the number of unique emails
     * using Gmail account matching.
     *
     * @param emails a list of strings received from the client.
     * @return an integer representing the number of unique and valid emails received from the client.
     */
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

    /**
     * Removes all .s and reformats emails with bangified host routes and % escaped mail routes
     * so that they only show the email. This is so the emails can be checked for uniqueness.
     *
     * @param email a string that contains a valid email
     * @return the email address of the recipient with all .s removed
     */
    String formatEmail(String email) {
        String ret = email.replace(".", "");
        ret = ret.replaceAll("\\([\\s\\S]*?\\)", "");
        ret = ret.replaceAll("\\+\\S*@", "@");
        if (ret.contains("!")) {
            boolean inQuotes = false;
            String iterVal = ret.split("@")[0];
            String[] split = new String[2];
            /*
            * Since emails with bangified host routes will not have .s in them,
            * they can have part enclosed in "s. This means that I need to see if the
            * ! appears out side of the "s. If it does then i need to change the email from
            * [destination uucp domain]![email account name]@[route domain] to:
            * [email account name]@[destination uucp domain]
            */
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

        /*
        * retrieves the recipients email out of escaped mail routed emails of the form
        * [email account name]%[domain of email account]@[routed email server]
        */
        if (ret.contains("%")) {
            String[] split = ret.split("@");
            split = split[0].split("%");
            return split[0] + "@" + split[1];
        }

        return ret;
    }

    /**
     * Removes comments from a string that contains a valid email. Otherwise returns null.
     * A comment may appear at the beginning of an email, the end of an email, just before the {@literal @},
     * or just after the {@literal @}. Comment in an email is of the form, ([comment text here]), and can contain
     * any characters in it, including a nested comment.
     *
     * @param email a string that may or may not be an email
     * @return either null if it is determined to not be an email or an email with comments removed
     */
    String removeComments(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }

        boolean inComment = false;
        boolean inQuotes = false;
        String[] parts = new String[2];
        /*
        * finds the first @ symbol not escaped or in "s and splits the string in two parts there
        */
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

        /*
        * Loops over both parts of the email (before and after the @)
        */
        for (int x = 0; x < parts.length; x++) {
            int parenDepth = 0;
            int end = -1;
            inQuotes = false;
            /*
            * Removes comments from the beginning of the part of the email.
            * Avoids escaped parenthesis and parenthesis in "s
             */
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
            /*
             * Removes comments from the end of the part of the email.
             * Avoids escaped parenthesis and parenthesis in "s
             */
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

    /**
     * Emails can be surrounded by {@literal <} {@literal >} characters. This function extracts the part
     * of the email that we wish to validate.
     *
     * @param email a string that may contain an email surround by {@literal <} {@literal >}
     * @return the string as is or the email if it was surrounded by {@literal <} {@literal >}
     */
    String unwrap(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int begin = -1;
        int end = -1;
        boolean inQuotes = false;
        /*
        * extracts the part of the string enclosed by < > if both of those characters occur.
        * If they appear in part of the email enclosed in double quotes they are to be ignored.
        */
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
