package com.example;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.StringTokenizer;

public class Main {
    static ArrayList<Integer>[] adj; // 인접 리스트
    static boolean[] visited;

    public static void main(String[] args) {
        Solution sol = new Solution();
        int[] result = sol.findNextGreaterElement(new int[]{2, 1, 5, 6, 2, 3});
        // 결과 출력: [5, 5, 6, -1, 3, -1]
        for (int r : result) {
            System.out.print(r + " ");
        }
    }

    static class Solution {
        public int[] findNextGreaterElement(int[] numbers) {
            int n = numbers.length;
            int[] answer = new int[n];
            Arrays.fill(answer, -1); // 결과를 -1로 초기화

            Stack<Integer> stack = new Stack<>(); // 인덱스를 저장할 스택

            for (int i = 0; i < n; i++) {
                // 스택이 비어있지 않고, 현재 값이 스택 top의 값보다 크다면
                while (!stack.isEmpty() && numbers[i] > numbers[stack.peek()]) {
                    answer[stack.pop()] = numbers[i]; // NGE를 찾았으므로 answer 갱신
                }
                stack.push(i); // 현재 인덱스를 스택에 push
            }

            return answer;
        }
    }
}