#!/bin/bash
sed -i 's/val vatAmount: Double\n)\n)/val vatAmount: Double\n)/g' app/src/main/java/com/boikhata/domain/model/Phase3Entities.kt
