package models

import java.sql.Timestamp

case class Recipe(
    id: Int,
    title: String,
    making_time: String,
    serves: String,
    ingredients: String,
    cost: Int,
    created_at: Timestamp,
    updated_at: Timestamp)

case class NewRecipe(
    title: String,
    making_time: String,
    serves: String,
    ingredients: String,
    cost: Int)