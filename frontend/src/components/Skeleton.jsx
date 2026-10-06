import React from 'react';
import styles from './Skeleton.module.css';

const Skeleton = ({
  width = '100%',
  height = '1rem',
  borderRadius = 'var(--radius-md)',
  variant = 'text', // 'text' | 'circle' | 'rect'
  className = '',
  style = {},
}) => {
  const variantClass = styles[variant] || styles.text;

  return (
    <div
      className={`${styles.skeleton} ${variantClass} ${className}`}
      style={{
        width,
        height: variant === 'circle' ? width : height,
        borderRadius: variant === 'circle' ? '50%' : borderRadius,
        ...style,
      }}
    />
  );
};

export default Skeleton;
