# Seam-Carving
Image reduction by seam carving. A small Kotlin program as part of Jetbrains Academy Kotlin Class that reduces the size of an image by removing seams with the lowest energy.

Command line parameters:
  -in <input filename>
  -out <output filename>
  -width <number of seams to remove vertically>
  -height <number of seems to remove horizontally>
  
  Example:
  Main.kt -in sky.png -out sky-reduced.png -width 125 -height 50
