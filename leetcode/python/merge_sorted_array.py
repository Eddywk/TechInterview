from typing import List

# Leetcode 88: https://leetcode.com/problems/merge-sorted-array/


def merge(self, nums1: List[int], m: int, nums2: List[int], n: int) -> None:
    """
    Do not return anything, modify nums1 in-place instead.
    """
    curr = m + n - 1
    m -= 1
    n -= 1

    while m >= 0 and n >= 0:
        if nums2[n] > nums1[m]:
            nums1[curr] = nums2[n]
            n -= 1
        else:
            nums1[curr] = nums1[m]
            m -= 1
        curr -= 1

    if n >= 0:
        nums1[0:n+1] = nums2[0:n+1]
