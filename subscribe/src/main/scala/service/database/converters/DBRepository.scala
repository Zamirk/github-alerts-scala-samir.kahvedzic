package service.database.converters

import doobie._
import service.domain.DatabaseModels._
import service.domain.DatabaseModels.RequestType._
import doobie.postgres.implicits._
import service.database.converters.DoobieGenericConverters.{From, To}

object RequestType {
  implicit val requestGet: Get[RequestType] =
    Get[String].temap(s =>
      (Option(s) collect {
        case OpenPR.dbString    => OpenPR
        case ApprovePR.dbString => MergePR
        case ApprovePR.dbString => ApprovePR
        case DeclinePR.dbString => DeclinePR
        case All.dbString       => All
      }).toRight(s"Value '$s' is not a valid RequestType")
    )

  implicit val requestPut: Put[RequestType] =
    Put[String].contramap {
      case OpenPR    => OpenPR.dbString
      case MergePR   => MergePR.dbString
      case ApprovePR => ApprovePR.dbString
      case DeclinePR => DeclinePR.dbString
      case All       => All.dbString
    }
}
object DBRepository {
  import RequestType.requestGet
  import RequestType.requestPut

  val repositoriesRead: Read[DBRepository]   = To[DBRepository].genericRead
  val repositoriesWrite: Write[DBRepository] = From[DBRepository].genericWrite

}
