package com.aero.myapplication

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.log


class MainActivity : AppCompatActivity() ,OnItemClickListener{

    private var newImageUrl: String? = null

    private lateinit var DialogLayout: View
    private lateinit var EditarDialogLayout: View
    private lateinit var db: FirebaseFirestore
    private lateinit var Prodadapter: ProductAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var searchView: androidx.appcompat.widget.SearchView
    private lateinit var priceSeekBar: SeekBar
    private lateinit var priceTextView: TextView
    private lateinit var sortSpinner: Spinner



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        searchView = findViewById(R.id.searchView)
        priceSeekBar = findViewById(R.id.priceSeekBar)
        priceTextView = findViewById(R.id.priceTextView)
        sortSpinner = findViewById(R.id.sortSpinner)

        setupSearchView()
        setupPriceSeekBar()
        setupSortSpinner()


        //validacion de usuario
        if (auth.currentUser == null) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
            return
        }


        //Configuración del RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.productsRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        Prodadapter = ProductAdapter(this)
        recyclerView.adapter = Prodadapter
        //Cargamos la lista de productos
        loadProducts()



        fun onEditClick(product: Product) {
            // Abre un diálogo o actividad para editar el producto
            EditarProductDialog(product)
        }

        fun onDeleteClick(product: Product) {
            // Lógica para eliminar el producto
            deleteProduct(product)
        }
       //boton agregar
        val addProductFab: FloatingActionButton = findViewById(R.id.addProductFab)
        addProductFab.setOnClickListener {
            RegistrarProducto(Prodadapter)
        }



    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Prodadapter.filter(newText ?: "", priceSeekBar.progress.toDouble())
                return true
            }
        })
    }

    private fun setupPriceSeekBar() {
        priceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                priceTextView.text = "Precio máximo: $${progress}"
                Prodadapter.filter(searchView.query.toString(), progress.toDouble())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSortSpinner() {
        val sortOptions = arrayOf("Más baratos primero", "Más caros primero", "A-Z", "Z-A")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortSpinner.adapter = adapter

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                Prodadapter.sort(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    private fun EditarProductDialog(product: Product) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Editar producto")

        val inflater = layoutInflater
        val EditarDialogLayout = inflater.inflate(R.layout.activity_alert_editarproducto, null)
        builder.setView(EditarDialogLayout)

        val nameEditText = EditarDialogLayout.findViewById<EditText>(R.id.edtNameEditar)
        val quantityEditText = EditarDialogLayout.findViewById<EditText>(R.id.edtQuantityEditar)
        val categoryEditText = EditarDialogLayout.findViewById<EditText>(R.id.edtCategoryEditar)
        val priceEditText = EditarDialogLayout.findViewById<EditText>(R.id.edtPriceEditar)
        val productImageView = EditarDialogLayout.findViewById<ImageView>(R.id.edtimageView)
        val btnSelectImage = EditarDialogLayout.findViewById<Button>(R.id.adtbtnCamara) // Botón para seleccionar imagen

        // Rellenar los campos con los datos del producto
        nameEditText.setText(product.name)
        quantityEditText.setText(product.quantity)
        categoryEditText.setText(product.category)
        priceEditText.setText(product.price)

        // Cargar la imagen actual del producto
        Glide.with(this)
            .load(product.imageUrl)
            .placeholder(R.drawable.imgcarga)
            .error(R.drawable.imgerror)
            .into(productImageView)

        // Almacenar la URL de la imagen nueva
        newImageUrl = null // Inicializa en null

        // Captura de imagen con la cámara
        btnSelectImage.setOnClickListener {
            // Llama al método para seleccionar una imagen
            startForResultEdit.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        val btnUpdate = EditarDialogLayout.findViewById<Button>(R.id.btnEditar)
        val mAlertDialog = builder.show()
        btnUpdate.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val quantity = quantityEditText.text.toString().trim()
            val category = categoryEditText.text.toString().trim()
            val price = priceEditText.text.toString().trim()

            // Verifica si se ha seleccionado una nueva imagen
            if (newImageUrl != null) {
                uploadImageToFirebase(newImageUrl!!) { imageUrl ->
                    updateProductInFirebase(product.id, name, quantity, category, price, imageUrl)
                }
            } else {
                // Usa la URL existente si no se seleccionó una nueva imagen
                updateProductInFirebase(product.id, name, quantity, category, price, product.imageUrl ?: "")
            }

            mAlertDialog.dismiss()
        }
    }



    // Función para guardar la imagen temporalmente y obtener un URI
    private fun saveImageAndReturnUri(imageBitmap: Bitmap): Uri {
        val file = File(applicationContext.cacheDir, "${System.currentTimeMillis()}.jpg")
        val fos = FileOutputStream(file)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
        fos.flush()
        fos.close()
        return Uri.fromFile(file)
    }

    // Subir imagen a Firebase
    private fun uploadImageToFirebase(imageUri: String, onSuccess: (String) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")

        val file = Uri.parse(imageUri)
        fileRef.putFile(file)
            .addOnSuccessListener { taskSnapshot ->
                fileRef.downloadUrl.addOnSuccessListener { uri ->
                    onSuccess(uri.toString()) // Devuelve la URL descargable de la imagen
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Actualizar producto en Firestore
    private fun updateProductInFirebase(productId: String, name: String, quantity: String, category: String, price: String, imageUrl: String) {
        val product = hashMapOf(
            "name" to name,
            "quantity" to quantity,
            "category" to category,
            "price" to price,
            "image" to imageUrl
        )

        db.collection("products").document(productId).set(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto actualizado con éxito", Toast.LENGTH_SHORT).show()
                loadProducts() // Actualizar la lista de productos
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }










    /*
        private fun EditarProductDialog(product: Product) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Editar producto")

            val inflater = layoutInflater
            DialogLayout = inflater.inflate(R.layout.activity_alert_editarproducto, null)
            builder.setView(DialogLayout)

            val nameEditText = DialogLayout.findViewById<EditText>(R.id.edtNameEditar)
            val quantityEditText = DialogLayout.findViewById<EditText>(R.id.edtQuantityEditar)
            val categoryEditText = DialogLayout.findViewById<EditText>(R.id.edtCategoryEditar)
            val priceEditText = DialogLayout.findViewById<EditText>(R.id.edtPriceEditar)
            val productImageView =DialogLayout.findViewById<ImageView>(R.id.edtimageView)
            val btnSelectImage = DialogLayout.findViewById<Button>(R.id.adtbtnCamara) // Botón para seleccionar imagen

            // Rellenar los campos con los datos del producto
            nameEditText.setText(product.name)
            quantityEditText.setText(product.quantity)
            categoryEditText.setText(product.category)
            priceEditText.setText(product.price)

            // Cargar la imagen actual
            Glide.with(this)
                .load(product.imageUrl)
                .placeholder(R.drawable.imgcarga)
                .error(R.drawable.imgerror)
                .into(productImageView)

            // Almacenar la URL de la imagen nueva
            newImageUrl = null // Inicializa en null

            btnSelectImage.setOnClickListener {
                // Llama al método para seleccionar una imagen
                startForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
            }

            val btnUpdate = DialogLayout.findViewById<Button>(R.id.btnEditar)
            val mAlertDialog = builder.show()
            btnUpdate.setOnClickListener {
                val name = nameEditText.text.toString().trim()
                val quantity = quantityEditText.text.toString().trim()
                val category = categoryEditText.text.toString().trim()
                val price = priceEditText.text.toString().trim()

                // Crea una variable temporal para almacenar el valor de newImageUrl
                val currentImageUrl = newImageUrl
                // Verifica si se ha seleccionado una nueva imagen
                if (currentImageUrl != null) {
                    uploadImageToFirebase(currentImageUrl) { imageUrl ->
                        updateProductInFirebase(product.id, name, quantity, category, price, imageUrl)
                    }
                } else {
                    // Usa la URL existente si no se seleccionó una nueva imagen
                    // Asegúrate de que product.imageUrl no sea null
                    updateProductInFirebase(product.id, name, quantity, category, price, product.imageUrl ?: "")
                }

                mAlertDialog.dismiss()
            }

        }*/
        // Firebase Storage reference
        val storageReference = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")
        // Subir imagen a Firebase Storage
        private fun uploadImageAndAddProduct(imageBitmap: Bitmap, name: String, quantity: String, category: String, price: String) {
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            // Sube la imagen a Firebase Storage
            val uploadTask = storageReference.putBytes(data)
            uploadTask.addOnSuccessListener {
                // Una vez subida la imagen, se puede guardar la referencia del producto
                storageReference.downloadUrl.addOnSuccessListener { uri ->
                    // Aquí llamamos a la función para agregar el producto, pero sin una URL explícita
                    addProductToFirebase(name, quantity, category, price, uri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
        }/*
        // Agregar producto con referencia de la imagen en Firestore
        private fun uploadImageToFirebase(imageUri: String, onSuccess: (String) -> Unit) {
            Log.d("ImageUpload", "Iniciando la carga de la imagen con URI: $imageUri")

            val storageRef = FirebaseStorage.getInstance().reference
            val fileRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")

            if (imageUri.isEmpty()) {
                Log.e("ImageUpload", "La URL de la imagen está vacía.")
                return
            }

            val file = Uri.parse(imageUri)
            if (file == null || file.scheme == null) {
                Log.e("ImageUpload", "La URI de la imagen es inválida.")
                return
            }

            fileRef.putFile(file)
                .addOnSuccessListener { taskSnapshot ->
                    Log.d("ImageUpload", "Imagen subida con éxito: ${taskSnapshot.bytesTransferred} bytes")
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ImageUpload", "Error al subir la imagen: ${e.message}")
                    Toast.makeText(this, "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
        private fun updateProductInFirebase(productId: String, name: String, quantity: String, category: String, price: String, imageUrl: String) {
            val product = hashMapOf(
                "name" to name,
                "quantity" to quantity,
                "category" to category,
                "price" to price,
                "image" to imageUrl
            )

            db.collection("products").document(productId).set(product)
                .addOnSuccessListener {
                    Toast.makeText(this, "Producto actualizado con éxito", Toast.LENGTH_SHORT).show()
                    loadProducts() // Actualizar la lista
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al actualizar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    */


    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val imageBitmap = intent?.extras?.get("data") as Bitmap
            // Usa el layout inflado del diálogo para buscar el ImageView
            val imageView = DialogLayout.findViewById<ImageView>(R.id.imageView)
            imageView?.setImageBitmap(imageBitmap)
        }
    }
    private val startForResultEdit = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: androidx.activity.result.ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val imageBitmap = intent?.extras?.get("data") as Bitmap
            // Usa el layout inflado del diálogo para buscar el ImageView
            val imageView = EditarDialogLayout.findViewById<ImageView>(R.id.imageView)
            imageView?.setImageBitmap(imageBitmap)
        }
    }


    private fun RegistrarProducto(adapter: ProductAdapter) {
        val titleAlertProduct = "Agregar nuevo producto"
        val builder = AlertDialog.Builder(this)
        builder.setTitle(titleAlertProduct)

        val inflater = layoutInflater
        DialogLayout = inflater.inflate(R.layout.activity_alert_addproducto, null) // Inicializa la variable de clase
        builder.setView(DialogLayout)

        val btnCamara = DialogLayout.findViewById<Button>(R.id.btnCamara)
        val btnCreate = DialogLayout.findViewById<Button>(R.id.btnCreate)
        val imageView = DialogLayout.findViewById<ImageView>(R.id.imageView)

        btnCamara.setOnClickListener {
            startForResult.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        }

        val mAlertDialog = builder.show()

        btnCreate.setOnClickListener {
            // Ahora obtenemos los campos desde el layout inflado (dialogLayout)
            val nameText = DialogLayout.findViewById<EditText>(R.id.edtName).text.toString().trim()
            val quantityText = DialogLayout.findViewById<EditText>(R.id.edtQuantity).text.toString().trim()
            val category = DialogLayout.findViewById<EditText>(R.id.edtCategory).text.toString().trim()
            val priceText = DialogLayout.findViewById<EditText>(R.id.edtPrice).text.toString().trim()

            // Validaciones
            if (nameText.isEmpty()) {
                Toast.makeText(this, "El nombre del producto no puede estar vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (quantityText.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa una cantidad", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (category.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa una categoría", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (priceText.isEmpty()) {
                Toast.makeText(this, "Por favor ingresa el precio del producto", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Subir la imagen y agregar el producto
            imageView.drawable?.let { drawable ->
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    uploadImageAndAddProduct(bitmap, nameText, quantityText, category, priceText)
                    mAlertDialog.dismiss()

                } else {
                    Toast.makeText(this, "Por favor toma una foto primero", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun addProductToFirebase(name: String, quantity: String, category: String, price: String, imageUrl: String) {
        val product = hashMapOf(
            "name" to name,
            "quantity" to quantity,
            "category" to category,
            "price" to price,
            "image" to imageUrl // Aquí se puede cambiar "image" para almacenar la URL de Firebase Storage
        )

        db.collection("products").add(product)
            .addOnSuccessListener {
                Toast.makeText(this, "Producto agregado con éxito", Toast.LENGTH_SHORT).show()
                loadProducts() // Actualiza la lista de productos
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }






    private fun loadProducts() {
        db.collection("products").get().addOnSuccessListener { result ->
            val products = result.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val quantity = doc.getString("quantity") ?: ""
                val price = doc.getString("price") ?: ""
                val category = doc.getString("category") ?: ""
                val image = doc.getString("image") ?: ""
                val id = doc.id
                Product(id, name, quantity, price, category, image)
            }

            Prodadapter.setProducts(products)

            // Configurar el valor máximo del SeekBar basado en el precio más alto
            val maxPrice = products.maxOfOrNull { it.price.toDoubleOrNull() ?: 0.0 }?.toInt() ?: 1000
            priceSeekBar.max = maxPrice
            priceTextView.text = "Precio máximo: $${maxPrice}"
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error al cargar los productos: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onEditClick(product: Product) {
        // Lógica para editar el producto
        EditarProductDialog(product)
        // Toast.makeText(this, "Editar producto: ${product.name}", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteClick(product: Product) {

        val builder = AlertDialog.Builder(this)

        // Título opcional
        builder.setTitle("Eliminar Producto")

        // Mensaje de la alerta
        builder.setMessage("Es tas seguro que quieres Eliminar este producto?")

        // Agregar botón de confirmación
        builder.setPositiveButton("Eliminar") { dialog, which ->
            // Acción a realizar cuando se presiona el botón
            deleteProduct(product)
            dialog.dismiss()  // Cierra el diálogo
        }
        // Agregar botón de cancelación
        builder.setNegativeButton("Cancelar") { dialog, which ->
            // Acción a realizar cuando se presiona el botón Cancelar
            //  Toast.makeText(applicationContext, "Operación cancelada", Toast.LENGTH_SHORT).show()
            dialog.dismiss()  // Cierra el diálogo
        }

        // Crear y mostrar el diálogo
        val alertDialog = builder.create()
        alertDialog.show()


        // Lógica para eliminar el producto
    }

    private fun deleteProduct(product: Product) {
        // Lógica para eliminar el producto de Firestore
        db.collection("products").document(product.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto ${product.name} \"eliminado", Toast.LENGTH_SHORT).show()
                loadProducts() // Actualizar la lista
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar el producto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
