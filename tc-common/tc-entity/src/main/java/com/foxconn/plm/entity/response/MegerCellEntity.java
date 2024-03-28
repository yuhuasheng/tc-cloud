package com.foxconn.plm.entity.response;

public class MegerCellEntity {
        public int startRow;
        public int endRow;

        public MegerCellEntity(int startRow, int endRow) {
            this.startRow = startRow;
            this.endRow = endRow;
        }
    }