package controllers

import scala.collection.mutable
import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import models._
import daos._

import play.api.db.slick._
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import java.time.Instant 
import java.sql.Timestamp

@Singleton
class RecipeController @Inject() (
protected val dbConfigProvider: DatabaseConfigProvider,
cc: ControllerComponents,
recipeDAO: RecipeDAO)(
    implicit ec: ExecutionContext
) extends AbstractController(cc) with HasDatabaseConfigProvider[JdbcProfile] {
    import profile.api._

    // TO-DO: Change Timestamp formatting from Long to String to make more user friendly
    def timestampToLong(t: Timestamp): Long = t.getTime
    def longToTimestamp(dt: Long): Timestamp = new Timestamp(dt)

    implicit val timestampFormat: Format[Timestamp] = new Format[Timestamp] {
        def writes(t: Timestamp): JsValue = Json.toJson(timestampToLong(t))
        def reads(json: JsValue): JsResult[Timestamp] = Json.fromJson[Long](json).map(longToTimestamp)
    }

    implicit val recipeJson = Json.format[Recipe]
    implicit val newRecipeJson = Json.format[NewRecipe]
    implicit val responseBaseJson = Json.format[ResponseBase]
    implicit val createErrorResponseJson = Json.format[CreateErrorResponse]
    implicit val recipeResponseJson = Json.format[RecipeResponse]
    implicit val recipeListResponseJson = Json.format[RecipeListResponse]

    val notFoundResponse = ResponseBase("No recipe found")

    def getAll() = Action.async { implicit request: Request[AnyContent] =>
        recipeDAO.all().map {
            recipes => 
                val responseList = RecipeListResponse(recipes)
                Ok(Json.toJson(responseList))
        }
    }
    
    def getRecipe(recipeId: Int) = Action.async { implicit request: Request[AnyContent] =>
        val queryResult = for {
            foundRecipe <- recipeDAO.findById(recipeId)
        } yield (foundRecipe)

        queryResult.map {
            foundRecipe =>
                foundRecipe match {
                    case Some(recipe) =>
                        val response = RecipeResponse("Recipe details by id", Seq(recipe))
                        Ok(Json.toJson(response))
                    case None => Ok(Json.toJson(notFoundResponse))
                }
        }
    }

    def addRecipe() = Action.async { implicit request: Request[AnyContent] =>
        val content = request.body 
        val jsonObject = content.asJson
        val todoListRecipe: Option[NewRecipe] = 
            jsonObject.flatMap( 
                Json.fromJson[NewRecipe](_).asOpt 
            )

        // TO-DO: Validate missing required fields to return proper message

        todoListRecipe match {
            case Some(n) =>
                val now = Timestamp.from(Instant.now());
                val recipeToAdd = Recipe(0, n.title, n.making_time, n.serves, n.ingredients, n.cost, now, now)
                val createdResponse = for {
                    createdRecipe <- recipeDAO.insert(recipeToAdd)
                } yield (createdRecipe)

                createdResponse.flatMap {
                    createdRecipe =>
                        val response = RecipeResponse("Recipe successfully created!", Seq(createdRecipe))
                        Future.successful(Ok(Json.toJson(response)))
                }
            case None =>
                val response = CreateErrorResponse("Recipe creation failed!", "title, making_time, serves, ingredients, cost")
                Future.successful(Ok(Json.toJson(response)))
        }
    }

    def patchRecipe(recipeId: Int) = Action.async { implicit request: Request[AnyContent] =>
        val content = request.body 
        val jsonObject = content.asJson
        val todoListRecipe: Option[NewRecipe] = 
            jsonObject.flatMap( 
                Json.fromJson[NewRecipe](_).asOpt 
            )
        
        // TO-DO: Support partial update ("patch"), current it requires all fields

        todoListRecipe match {
            case Some(n) =>
                val queryResult = for {
                    foundRecipe <- recipeDAO.findById(recipeId)
                } yield (foundRecipe)

                queryResult.flatMap {
                    foundRecipe =>
                        foundRecipe match {
                            case Some(recipe) =>
                                val now = Timestamp.from(Instant.now());
                                val updatedRecipe = recipe.copy(
                                    title=n.title,
                                    making_time=n.making_time,
                                    serves=n.serves,
                                    ingredients=n.ingredients,
                                    cost=n.cost,
                                    updated_at=now)
                                for {
                                    _ <- recipeDAO.patchById(recipeId, updatedRecipe)
                                } yield Ok(Json.toJson(RecipeResponse("Recipe successfully updated!", Seq(updatedRecipe))))
                            case None =>
                                Future.successful(Ok(Json.toJson(notFoundResponse)))
                        }
                }
            case None =>
                val response = ResponseBase("Recipe update failed, invalid body!")
                Future.successful(Ok(Json.toJson(response)))
        }
    }

    def deleteRecipe(recipeId: Int) = Action.async { implicit request: Request[AnyContent] =>
        val queryResult = for {
            foundRecipe <- recipeDAO.findById(recipeId)
        } yield (foundRecipe)

        queryResult.flatMap {
            foundRecipe =>
                foundRecipe match {
                    case Some(recipe) => 
                        for {
                            _ <- recipeDAO.deleteById(recipeId)
                        } yield Ok(Json.toJson(ResponseBase("Recipe successfully removed!")))
                    case None =>
                        Future.successful(Ok(Json.toJson(notFoundResponse)))
                }
        }
    }
}