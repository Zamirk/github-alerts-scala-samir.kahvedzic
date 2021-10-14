package service.models

object DomainSamples {

  object SlackResponseBody {

    val jsonString = """
                       |
                       |{
                       |  "response_type":"in_channel",
                       |  "text":"It's 80 degrees right now.",
                       |  "blocks":[
                       |    {
                       |      "type":"section",
                       |      "text":{
                       |        "type":"mrkdwn",
                       |        "text":"*It's 80 degrees right now.*"
                       |      }
                       |    },
                       |    {
                       |      "type":"section",
                       |      "text":{
                       |        "type":"mrkdwn",
                       |        "text":"Partly cloudy today and tomorrow"
                       |      }
                       |    }
                       |  ]
                       |}
                       |
                       |
                       |"""
  }
}
