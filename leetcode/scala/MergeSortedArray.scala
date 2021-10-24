object MergeSortedArray {
  // Leetcode 88: https://leetcode.com/problems/merge-sorted-array/
  def merge(nums1: Array[Int], m: Int, nums2: Array[Int], n: Int): Unit = {
    var curr = m + n - 1
    var (i, j) = (m - 1, n - 1)

    while (i >= 0 && j >= 0) {
      if (nums1(i) < nums2(j)) {
        nums1(curr) = nums2(j)
        j -= 1
      } else {
        nums1(curr) = nums1(i)
        i -= 1
      }
      curr -= 1
    }

    while (j >= 0) {
      nums1(curr) = nums2(j)
      j -= 1
      curr -= 1
    }
  }
}