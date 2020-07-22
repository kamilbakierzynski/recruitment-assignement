# Are you afraid of the dark?

### Used tools
I've chosen to write a solution using Scala with Akka Actors.
It allows me to use parallelism needed to achieve good results even
when the system is on heavy load. System is creating new actor for every photo
and every worker is responsible of opening, calculating and saving image.
There is one actor called boss - responsible of providing tasks for workers.

### Configuration
In the `application.conf` user can specify path to samples and path to output.
In the same spot you can adjust threshold value for "cut off" point.
To the images that were provided with the task I added all black and all white to test
if the algorithm gives them respectively 100 and 0 points. There are also couple
images from the web I have downloaded from the internet.

### Summary
All the images provided with the task are classified correctly
however I have specified "cut off" point at around 70. It's high but
most of the images in the dark folder with samples get scores around 98 (lowest score 84) so the
70 should do the work. It's easily accessible so it's no problem to tweak on the go.

### Solution description
To calculate the "brightness" i use the V (value) from the HSV color scheme.
It simple to calculate - max value of red, green, blue divided by 255. After that
I get the average of previously calculated value of every pixel in the image. The result
is presented as percentage from 0 (black) to 100 (white). I take the opposite of that
to get the desired score.

# Score analysis
>> All images that were provided are correctly classified (14 in each class + all white and all black for 0 and 100 score respectively)

| Actual/Result | Bright | Dark |
|---------------|--------|------|
| Bright        | 15     | 0    |
| Dark          | 0      | 15   |




