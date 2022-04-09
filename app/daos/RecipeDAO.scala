package daos

import scala.concurrent.{ ExecutionContext, Future }
import javax.inject.Inject

import models.Recipe
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import java.sql.Timestamp

class RecipeDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Recipes = TableQuery[RecipesTable]

  def all(): Future[Seq[Recipe]] = db.run(Recipes.result)
  
  def findById(id: Int): Future[Option[Recipe]] =
    db.run(Recipes.filter(_.id === id).result.headOption)

  def insert(recipe: Recipe): Future[Recipe] = db.run((Recipes returning Recipes.map(_.id)
         into ((recipe,id) => recipe.copy(id=id))
  ) += recipe).map { recipe => recipe }

  def deleteById(id: Int): Future[Unit] =
    db.run(Recipes.filter(_.id === id).delete).map(_ => ())

 def patchById(id: Int, recipe: Recipe): Future[Unit] = {
    db.run(
      Recipes.filter(_.id === id)
      .map(r => (r.title,r.making_time,r.serves,r.ingredients,r.cost))
      .update(recipe.title, recipe.making_time, recipe.serves, recipe.ingredients, recipe.cost))
    .map(_ => ())
  }

  private class RecipesTable(tag: Tag) extends Table[Recipe](tag, "recipes") {

    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def making_time = column[String]("making_time")
    def serves = column[String]("serves")
    def ingredients = column[String]("ingredients")
    def cost = column[Int]("cost")
    def created_at = column[Timestamp]("created_at", O.SqlType("timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"))
    def updated_at = column[Timestamp]("updated_at", O.SqlType("timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP"))

    def * = (id, title, making_time, serves, ingredients, cost, created_at, updated_at) <> (Recipe.tupled, Recipe.unapply)
  }
}