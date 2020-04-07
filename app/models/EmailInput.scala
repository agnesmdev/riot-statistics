package models

import com.mailjet.client.resource.Emailv31.Message
import org.json.{JSONArray, JSONObject}


case class Email(from: EmailParty,
                 to: EmailParty,
                 subject: String,
                 textPart: String,
                 htmlPart: String,
                 customId: String) {
  def toJSONObject: JSONObject = new JSONObject()
    .put(Message.FROM, from.toJsonObject)
    .put(Message.TO, new JSONArray().put(to.toJsonObject))
    .put(Message.SUBJECT, subject)
    .put(Message.TEXTPART, textPart)
    .put(Message.HTMLPART, htmlPart)
    .put(Message.CUSTOMID, customId)
}

case class EmailParty(email: String, name: String) {
  def toJsonObject: JSONObject = new JSONObject().put("Email", email).put("Name", name)
}

