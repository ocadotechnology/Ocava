This doc outlines the calculations for creating constant jerk traversals that via the `ConstantJerkSectionsFactory`.

When we assume the bot can maintain constant jerk with limits for acceleration, deceleration and speed, there are 8 possible motion profiles that the bot can attain depending on the values for these limits and the distance to travel. This is because for acceleration, deceleation and speed we can either reach the limit, or not, hence there are $2^3=8$ possible situations.

## Case 1 - Doesn't reach max acceleration, deceleration or speed

In the situation where we do not reach maximum acceleration, deceleration or the max speed, the accleration-time profile would look something like this:

      /\
     /  \  
    /    \    /
          \  / 
           \/

There are 4 stages, acceleration jerk up, acceleration jerk down, deceleration jerk up, deceleration jerk down where the jerk values are are $j_1, j_2, j_3, j_4$ and last for $t_1, t_2, t_3, t_4$ seconds respectively.

$$s = ut + \frac{1}{2}a.t^2 + \frac{1}{6}jt^3\\
s = vt - \frac{1}{2}a.t^2 - \frac{1}{3}jt^3$$

Assuming that acceleration is 0 after the acceleration jerk down section and 0 at the end of the traversal, we have:

$$\begin{aligned}
a & = j_1t_1 = -j_2t_2  \\
d & = j_3t_3 = -j_4t_4 \\
\end{aligned}$$

Speeds at the end of each section:
$$\begin{aligned}
v_1 & = \frac{1}{2}j_1t_1^2 = \frac{1}{2}\frac{a^2}{j_1}\\
v_2 & = v_1 - \frac{1}{2}j_2t_2^2 =  v_1 - \frac{1}{2}\frac{a^2}{j_2}\\
v_3 & = v_2 +\frac{1}{2}j_3t_3^2 =  v_2 +\frac{1}{2}\frac{d^2}{j_3}\\
v_4 & = v_3 - \frac{1}{2}j_4t_4^2 = v_3 - \frac{1}{2}\frac{d^2}{j_4} = 0\\
\end{aligned}$$
Distance travelled by each section:
$$\begin{aligned}
s_1 & = 0 + 0 + \frac{1}{6}j_1t_!^3 \\
s_2 & = \frac{1}{2}\frac{a^2}{j_1}t_2 + \frac{1}{2}j_1t_1t_2^2 + \frac{1}{6}j_2t_2^3 \\
& = -\frac{1}{2}\frac{a^3}{j_1j_2} + \frac{1}{2}\frac{a^3}{j_2^2} - \frac{1}{6}\frac{a^3}{j_2} \\
& = -\frac{1}{2}\frac{a^3}{j_1j_2} + \frac{1}{3}\frac{a^3}{j_2^2} \\
s_3 &= v_3t_3 - 0 - \frac{1}{3}j_3t_3^3 \\
&= \frac{1}{2}\frac{d^3}{j_3j_4} - \frac{1}{3}\frac{d^3}{j_3^2}\\
s_4 &= v_4t_t - \frac{1}{2}dt_4^2 - \frac{1}{3}j_4t_4^3\\
&= 0 - \frac{1}{2}\frac{d^3}{j_4^2} + \frac{1}{3}\frac{d^3}{j_4^2} \\
&= - \frac{1}{6}\frac{d^3}{j_4^2}
\end{aligned}$$

Since $v_1+v_2+v_3+v_4=0$ we have:

$$\begin{aligned}
\left(\frac{1}{j_1} - \frac{1}{j_2}\right)a^2 = \left(\frac{1}{j_4} - \frac{1}{j_3}\right)d^2
\end{aligned}$$

Using $S=s_1+s_2+s_3+s_4$ we have:

$$\begin{aligned}
S &= \left(\frac{1}{6}j_2t_2^3 -\frac{1}{2}\frac{a^3}{j_1j_2} + \frac{1}{3}\frac{a^3}{j_2^2}\right)a^3 + \left(\frac{1}{2}\frac{d^3}{j_3j_4} - \frac{1}{3}\frac{d^3}{j_3^2} - \frac{1}{6}\frac{d^3}{j_4^2}\right)d^3
\end{aligned}$$
Now let:
$$\begin{aligned}
A &= \frac{1}{j_1} - \frac{1}{j_2}   \\
B &= \frac{1}{j_4} - \frac{1}{j_3}   \\
C &= \frac{1}{6}j_2t_2^3 -\frac{1}{2}\frac{a^3}{j_1j_2} + \frac{1}{3}\frac{a^3}{j_2^2}   \\
D &= \frac{1}{2}\frac{d^3}{j_3j_4} - \frac{1}{3}\frac{d^3}{j_3^2} - \frac{1}{6}\frac{d^3}{j_4^2}   \\
\end{aligned}$$


So:
$$\begin{aligned}
A^2a&= Bd^2\\
S &= Ca^3 + Dd^3 \\
a &= \left(\frac{s}{C + D(\frac{A}{B})^{\frac{3}{2}}}\right)^{\frac{1}{3}} \\
d &= \left(\frac{s}{D + C(\frac{B}{A})^{\frac{3}{2}}}\right)^{\frac{1}{3}} \\
\end{aligned}$$

Thus we have calculated the maximum acceleration and deceleration reached by this sort of motion profile, however we need to check if it is achievable by the vehicle given its constraints:

$$\begin{aligned}
0 \lt a &\leq vehicleProperties.maxAcceleration \\
0 \gt d &\geq vehicleProperties.maxDeceleration \\
v_2 &\leq vehicleProperties.maxSpeed \\
\end{aligned}$$

If any of these constraints are violated, it implies that a traversal for a distance $S$ where we do not reach any of max acceleration, max deceleration or max speed is not possible.

// TODO other cases #458
