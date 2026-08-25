#!/bin/bash
sed -i 's/val isSales = user?.role == Role.SALES/val isSales = user?.role == Role.SALES\n    var showMaster by remember { mutableStateOf(false) }\n    val masterViewModel: MasterCatalogViewModel = hiltViewModel()/g' app/src/main/java/com/boikhata/presentation/catalog/CatalogScreen.kt

sed -i 's/Button(onClick = { showAddDialog = true }) { Text("+ নতুন বই") }/Button(onClick = { showAddDialog = true }) { Text("+ নতুন বই") }\n                Spacer(modifier = Modifier.width(8.dp))\n                Button(onClick = { showMaster = true }) { Text("মাস্টার ক্যাটালগ") }/g' app/src/main/java/com/boikhata/presentation/catalog/CatalogScreen.kt

cat << 'INNER_EOF' >> app/src/main/java/com/boikhata/presentation/catalog/CatalogScreen.kt

@Composable
fun MasterCatalogDialog(viewModel: MasterCatalogViewModel, onDismiss: () -> Unit) {
    val books by viewModel.books.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f),
        title = { Text("মাস্টার ক্যাটালগ (NCTB)") },
        text = {
            LazyColumn {
                items(books) { b ->
                    Card(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                        Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(b.titleBn)
                                Text("${b.classLevel} - ${b.subject}")
                            }
                            Button(onClick = { 
                                viewModel.importBook(b, b.mrp * 0.8, 10)
                                onDismiss()
                             }) { Text("ইম্পোর্ট (৳${com.boikhata.util.BengaliUtils.toBn(b.mrp)})") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("বন্ধ করুন") }
        }
    )
}
INNER_EOF

sed -i 's/if (showAddDialog) {/if (showMaster) { MasterCatalogDialog(masterViewModel) { showMaster = false } }\n\n    if (showAddDialog) {/g' app/src/main/java/com/boikhata/presentation/catalog/CatalogScreen.kt

