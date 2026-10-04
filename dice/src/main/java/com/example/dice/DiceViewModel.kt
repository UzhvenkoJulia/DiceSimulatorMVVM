package com.example.dice

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class DiceViewModel : ViewModel() {

    // список з 5 чисел (для кожної кістки)
    private val _diceValues = MutableLiveData<List<Int>>(List(5) { 1 })
    val diceValues: LiveData<List<Int>> get() = _diceValues

    // true - кістки крутяться, false - зупинилися
    private val _isRolling = MutableLiveData(false)
    val isRolling: LiveData<Boolean> get() = _isRolling

    private val activeThreads = mutableListOf<Thread>()

    @Volatile
    private var isCancelled = false

    fun rollDice() {
        if (_isRolling.value == true) return

        _isRolling.value = true
        isCancelled = false
        activeThreads.clear()

        // до 0 - всі кістки зупинилися
        val activeCount = AtomicInteger(5)

        val currentValues = _diceValues.value?.toIntArray() ?: IntArray(5) { 1 }

        for (i in 0 until 5) {
            val duration = 1500L + (i * 500L)

            val t = thread(start = false) {
                val iterations = (duration / 100).toInt()

                for (j in 0 until iterations) {
                    if (isCancelled) break

                    currentValues[i] = (1..6).random()

                    // synchronized, щоб потоки не побилися за дані
                    // postValue безпечно передає дані з фонового потоку на головний екран
                    synchronized(currentValues) {
                        _diceValues.postValue(currentValues.toList())
                    }

                    try {
                        Thread.sleep(100) // ефект анімації
                    } catch (e: InterruptedException) {
                        break
                    }
                }

                if (activeCount.decrementAndGet() == 0 && !isCancelled) {
                    _isRolling.postValue(false)
                }
            }
            activeThreads.add(t)
            t.start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        isCancelled = true
        activeThreads.forEach { it.interrupt() }
        activeThreads.clear()
    }
}