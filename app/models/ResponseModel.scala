package models

case class ResponseBase(message: String)
case class RecipeListResponse(recipes: Seq[Recipe])
case class RecipeResponse(message: String, recipe: Seq[Recipe])
case class CreateErrorResponse(message: String, required: String)