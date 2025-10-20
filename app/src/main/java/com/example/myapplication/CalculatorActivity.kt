package com.example.myapplication

import android.widget.Button
import android.widget.TextView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CalculatorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculator)

        val tvResult: TextView = findViewById(R.id.result_text_view)

        val buttonNumber = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
            R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9
        )
        for (id in buttonNumber) {
            val button: Button = findViewById(id)
            if (id == R.id.button1) {
                button.setOnClickListener{
                    tvResult.text = tvResult.text.toString() + button.text.toString()
                    button.setBackgroundColor(2090)
                }
            }
            else {
                button.setOnClickListener {
                    tvResult.text = tvResult.text.toString() + button.text.toString()
                }
            }
        }

        val buttonPlus: Button = findViewById(R.id.buttonPlus)
        buttonPlus.setOnClickListener {
            tvResult.text = tvResult.text.toString() + '+'
        }
        val buttonMinus: Button = findViewById(R.id.buttonMinus)
        buttonMinus.setOnClickListener {
            tvResult.text = tvResult.text.toString() + '-'
        }
        val buttonMultiply: Button = findViewById(R.id.buttonMultiply)
        buttonMultiply.setOnClickListener {
            tvResult.text = tvResult.text.toString() + '*'
        }
        val buttonDivide: Button = findViewById(R.id.buttonDivide)
        buttonDivide.setOnClickListener {
            tvResult.text = tvResult.text.toString() + '/'
        }
        val buttonClear: Button = findViewById(R.id.buttonClear)
        buttonClear.setOnClickListener {
            tvResult.text = " "
        }
        val buttonEqually: Button = findViewById(R.id.buttonEqually)
        buttonEqually.setOnClickListener {
            val expression = tvResult.text.toString()
            val result = calculateExpression(expression)
            tvResult.text = result
        }
    }

    private fun calculateExpression(expression: String): String {
        if (expression.isEmpty()) return " "
        var operator = ' '
        var operatorIndex = -1

        for (i in expression.indices) {
            val char = expression[i]
            if (char == '+' || char == '-' || char == '/' || char == '*') {
                operator = char
                operatorIndex = i
                break
            }
        }
        if (operatorIndex == -1) return expression

        val firstNumstr = expression.substring(0, operatorIndex)
        val secondNumstr = expression.substring(operatorIndex + 1)

        if (firstNumstr.isEmpty() || secondNumstr.isEmpty()) {
            return "Введите данные корректно!"
        }
        val firstNum = firstNumstr.toDoubleOrNull()
        val secondNum = secondNumstr.toDoubleOrNull()
        if (firstNum == null || secondNum == null) {
            return "Ошибка преобразования в дробное число, очистите ввод и попробуйте заново!"
        }

        val result = when (operator) {
            '+' -> firstNum + secondNum
            '-' -> firstNum - secondNum
            '*' -> firstNum * secondNum
            '/' -> {
                if (secondNum == 0.0) {
                    return "Деление на 0 запрещено, возвращайтесь в школу!"
                }
                firstNum / secondNum
            }
            else -> return "Так нельзя делать!"
        }
        return if (result % 1 == 0.0) result.toInt().toString() else result.toString()
    }
}
