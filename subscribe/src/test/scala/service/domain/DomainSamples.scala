package service.domain

object DomainSamples {

  object SlackResponseBody {

    val jsonString = s"""
                       
                       {
                         "response_type":"in_channel",
                         "text":"It's 80 degrees right now.",
                         "blocks":[
                           {
                             "type":"section",
                             "text":{
                               "type":"mrkdwn",
                               "text":"*It's 80 degrees right now.*"
                             }
                           },
                           {
                             "type":"section",
                             "text":{
                               "type":"mrkdwn",
                               "text":"Partly cloudy today and tomorrow"
                             }
                           }
                         ]
                       }
                       
                       
                       """
  }

  val jsonString2 = s"""
                     
                     {
                       "subscriptions" : [
                         {
                           "organization" : "47deg",
                           "repository" : "thool",
                           "subscribedAt" : "2000-10-10T10:10:10.00000001+01:00"
                         },
                         {
                           "organization" : "higherkindness",
                           "repository" : "skeuomorph",
                           "subscribedAt" : "2000-10-10T10:10:10.00000001+01:00"
                         },
                         {
                           "organization" : "47degrees",
                           "repository" : "github4s",
                           "subscribedAt" : "2000-10-10T10:10:10.00000001+01:00"
                         }
                       ]
                     }
                     
                     
                     """

}
