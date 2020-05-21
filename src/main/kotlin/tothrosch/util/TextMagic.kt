package tothrosch.util


/*
object TextMagic {
    // Read API key and phone number from environment variables
    private val apiKey = System.getenv("TEXT_MAGIC_API_KEY") ?: "defaultApiKey"
    private val phoneNumber = System.getenv("TEXT_MAGIC_PHONE_NUMBER") ?: "defaultPhoneNumber"

    val restClient: RestClient = RestClient("andreastoth", apiKey)

    fun sendSMS(text: String) {
        try {
            val message: TMNewMessage = restClient.getResource(TMNewMessage::class.java)
            message.setText(text)
            message.setPhones(arrayListOf(phoneNumber))
            message.send()
        } catch (ex: Exception) {
            println("couldn't send sms")
        }
    }
}
*/
