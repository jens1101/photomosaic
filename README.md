# Java Photo Mosaic
This CLI program creates a photo mosaic from a specified source image and an
image library. Currently there is no option to change the size of the mosaic
via the command line.

## Usage Example
After compiling the source code you can run the following:
```shell script
java Main ./resources/seagull.jpg ./resources/image_dataset/ ./seagull_mosaic.jpg
```

Here we assume that there is a "resources" folder in the same folder where the
`Main.class` file is. This resources folder contains the source image and the
image library to use.

In this example the final image will be a JPEG image named `seagull_mosaic.jpg` 
within the current folder.

### Example image library
In the example above I used the
[Caltech 101](http://www.vision.caltech.edu/Image_Datasets/Caltech101/) image 
dataset.

### Example image
In the example above I used
[this stock image](https://pixabay.com/photos/seagull-gabbiano-bird-fly-animal-293699/).