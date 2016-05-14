package controllers

import javax.inject._

import models.{Product, Products}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText}
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, Controller, Flash}

/**
  * Created by Sebo on 2016-05-06.
  */
@Singleton
class ProductController @Inject()(val messagesApi: MessagesApi) extends Controller with I18nSupport {

  /**
    * Create an Action to render an HTML page with a welcome message.
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def list = Action { implicit request =>
      val products = Products.findAll

      Ok(views.html.products.list(products))
  }

  def show(ean: Long) = Action { implicit request =>
    Products.findByEan(ean).map { product =>
      Ok(views.html.products.details(product))
    }.getOrElse(NotFound)
  }

  private val productForm: Form[Product] = Form(
    mapping(
      "ean" -> longNumber.verifying("validation.ean.duplicate", Products.findByEan(_).isEmpty),
      "name" -> nonEmptyText,
      "description" -> nonEmptyText
    )(Product.apply)(Product.unapply)
  )

  def newProduct = Action { implicit request =>
    val form = if (request2flash.get("error").isDefined)
      productForm.bind(request2flash.data)
    else
      productForm

    Ok(views.html.products.editProduct(form))
  }

  def save = Action { implicit request =>
    val newProductForm = productForm.bindFromRequest()

    newProductForm.fold(
      hasErrors = { form =>
        Redirect(routes.ProductController.newProduct())
          .flashing(
            Flash(form.data + ("error" -> Messages("validation.errors")))
          )
      },

      success = { product =>
        Products.add(product)

        val message = Messages("products.new.success", product.name)
        Redirect(routes.ProductController.show(product.ean))
          .flashing("success" -> message)
      }
    )
  }
}
