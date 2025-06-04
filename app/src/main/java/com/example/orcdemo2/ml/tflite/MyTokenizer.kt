package com.example.orcdemo2.ml.tflite


import android.content.res.AssetManager
import java.io.BufferedReader
import java.io.InputStreamReader

data class TokenizationResult(
    val inputIds: IntArray,
    val attentionMask: IntArray,
    val tokens: List<String>
)

class MyTokenizer(
    private val vocab: Map<String, Int>,
    private val unkToken: String = "[UNK]",
    private val clsToken: String = "[CLS]",
    private val sepToken: String = "[SEP]",
    private val padToken: String = "[PAD]"
) {

    fun tokenize(text: String, maxLen: Int = 128): TokenizationResult {
        val words = text.split(Regex("\\s+"))
        val tokens = mutableListOf<String>()

        for (word in words) {
            tokens += wordpieceTokenize(word)
        }

        // Add special tokens
        val finalTokens = mutableListOf<String>()
        finalTokens += clsToken
        finalTokens += tokens
        finalTokens += sepToken

        // Truncate if necessary
        if (finalTokens.size > maxLen) {
            finalTokens.subList(maxLen - 1, finalTokens.size).clear()
            finalTokens[finalTokens.lastIndex] = sepToken
        }

        val inputIds = IntArray(maxLen) { 0 }
        val attentionMask = IntArray(maxLen) { 0 }

        for (i in finalTokens.indices) {
            val token = finalTokens[i]
            inputIds[i] = vocab[token] ?: vocab[unkToken] ?: 0
            attentionMask[i] = 1
        }

        return TokenizationResult(
            inputIds = inputIds,
            attentionMask = attentionMask,
            tokens = finalTokens
        )
    }

    private fun wordpieceTokenize(word: String): List<String> {
        val tokens = mutableListOf<String>()
        var remaining = word.lowercase()
        var start = 0
        var found: Boolean

        while (start < remaining.length) {
            var end = remaining.length
            found = false

            while (start < end) {
                var sub = remaining.substring(start, end)
                if (start > 0) sub = "##$sub"
                if (vocab.containsKey(sub)) {
                    tokens.add(sub)
                    start = end
                    found = true
                    break
                }
                end -= 1
            }

            if (!found) {
                tokens.add(unkToken)
                break
            }
        }

        return tokens
    }

    companion object {
        fun loadFromAssets(assetManager: AssetManager, fileName: String): MyTokenizer {
            val vocab = mutableMapOf<String, Int>()
            val inputStream = assetManager.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            var index = 0
            reader.forEachLine { line ->
                vocab[line.trim()] = index++
            }
            reader.close()
            return MyTokenizer(vocab)
        }
    }
}
