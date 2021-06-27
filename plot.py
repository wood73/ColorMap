import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D

f = open("E:\\seam\\ColorMap\\s.txt", 'r')
data = f.readlines()

fig = plt.figure(figsize=(4, 4))
ax = fig.add_subplot(111, projection='3d')
ax.set_xlabel('Red', fontsize=20)
ax.set_ylabel('Green', fontsize=20)
ax.set_zlabel('blue', fontsize=20)

#  ax.scatter(10, 100, 200)
for i in data:
    point = i.split(",")
    ax.scatter(int(point[0]), int(point[1]), int(point[2]))  # plot the point (2,3,4) on the figure


plt.show()