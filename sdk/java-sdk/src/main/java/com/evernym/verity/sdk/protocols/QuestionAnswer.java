package com.evernym.verity.sdk.protocols;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.evernym.verity.sdk.exceptions.UndefinedContextException;
import com.evernym.verity.sdk.exceptions.WalletException;
import com.evernym.verity.sdk.utils.Context;

import com.evernym.verity.sdk.utils.Util;
import org.bouncycastle.jcajce.provider.digest.SHA3;
import org.bouncycastle.jcajce.provider.digest.SHA3.DigestSHA3;
import org.bouncycastle.util.encoders.Hex;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Builds and sends a new encrypted agent message for the Question protocol.
 */
public class QuestionAnswer extends Protocol {

    final private static String MSG_FAMILY = "question-answer";
    final private static String MSG_FAMILY_VERSION = "0.1";

    // Messages
    @SuppressWarnings("WeakerAccess")
    public static String QUESTION = "question";

    // Status Definitions
    public static Integer QUESTION_SENT_STATUS = 0;
    public static Integer QUESTION_ANSWERED_STATUS = 1;

    String connectionId;
    String notificationTitle;
    String questionText;
    String questionDetail;
    JSONArray validResponses;

    /**
     * Create a new Question object
     * @param connectionId the pairwise DID of the connection you want to send the question to
     * @param notificationTitle the title of the push notification (currently only rendered in Connect.Me when questionDetail is omitted)
     * @param questionText The main text of the question (included in the message the Connect.Me user signs with their private key)
     * @param questionDetail Any additional information about the question
     * @param validResponses The possible responses. See the Verity Protocol documentation for more information on how Connect.Me will render these options.
     */
    @SuppressWarnings("WeakerAccess")
    public QuestionAnswer(String connectionId, String notificationTitle, String questionText, String questionDetail,
            String[] validResponses) {
        super();
        this.connectionId = connectionId;
        this.notificationTitle = notificationTitle;
        this.questionText = questionText;
        this.questionDetail = questionDetail;
        this.validResponses = formatValidResponses(questionText, validResponses);

        defineMessages();
    }

    public static String getMessageType(String msgName) {
        return Util.getMessageType(QuestionAnswer.MSG_FAMILY, QuestionAnswer.MSG_FAMILY_VERSION, msgName);
    }

    public static String getProblemReportMessageType() {
        return Util.getProblemReportMessageType(QuestionAnswer.MSG_FAMILY, QuestionAnswer.MSG_FAMILY_VERSION);
    }

    public static String getStatusMessageType() {
        return Util.getStatusMessageType(QuestionAnswer.MSG_FAMILY, QuestionAnswer.MSG_FAMILY_VERSION);
    }

    private JSONArray formatValidResponses(String questionText, String[] validResponses) {
        JSONArray formattedValidResponses = new JSONArray();
        for (String validResponseString : validResponses) {
            JSONObject validResponse = new JSONObject();
            validResponse.put("text", validResponseString);
            validResponse.put("nonce", getHashedMessage(questionText, validResponseString));
            formattedValidResponses.put(validResponse);
        }
        return formattedValidResponses;
    }

    @Override
    protected void defineMessages() {
        JSONObject message = new JSONObject();
        message.put("@type", QuestionAnswer.getMessageType(QuestionAnswer.QUESTION));
        message.put("@id", QuestionAnswer.getNewId());
        message.put("connectionId", this.connectionId);
            JSONObject question = new JSONObject();
            question.put("notification_title", this.notificationTitle);
            question.put("question_text", this.questionText);
            question.put("question_detail", this.questionDetail);
            question.put("valid_responses", this.validResponses);
            message.put("question", question);
        this.messages.put(QuestionAnswer.QUESTION, message);
    }

    private String getHashedMessage(String questionText, String responseOption) {
        DigestSHA3 sha3256 = new SHA3.Digest256();
        sha3256.update(questionText.getBytes(StandardCharsets.UTF_8));
        sha3256.update(responseOption.getBytes(StandardCharsets.UTF_8));
        sha3256.update(UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        return Hex.toHexString(sha3256.digest());
    }


    /**
     * Sends the question message to Verity
     * @param context an instance of Context configured with the results of the provision_sdk.py script
     * @throws IOException               when the HTTP library fails to post to the agency endpoint
     * @throws UndefinedContextException when the context doesn't have enough information for this operation
     * @throws WalletException when there are issues with encryption and decryption
     */
    @SuppressWarnings("WeakerAccess")
    public byte[] ask(Context context) throws IOException, UndefinedContextException, WalletException {
        return this.send(context, this.messages.getJSONObject(QuestionAnswer.QUESTION));
    }
}